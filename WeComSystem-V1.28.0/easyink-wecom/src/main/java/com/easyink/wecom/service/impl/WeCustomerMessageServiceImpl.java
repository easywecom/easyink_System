package com.easyink.wecom.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.easyink.common.constant.WeConstans;
import com.easyink.common.core.domain.model.LoginUser;
import com.easyink.common.core.redis.RedisCache;
import com.easyink.common.enums.*;
import com.easyink.common.exception.CustomException;
import com.easyink.common.utils.StringUtils;
import com.easyink.common.utils.spring.SpringUtils;
import com.easyink.wecom.client.WeCustomerMessagePushClient;
import com.easyink.wecom.domain.WeCustomer;
import com.easyink.wecom.domain.WeCustomerMessage;
import com.easyink.wecom.domain.WeCustomerMessgaeResult;
import com.easyink.wecom.domain.dto.WeMediaDTO;
import com.easyink.wecom.domain.dto.common.AttachmentParam;
import com.easyink.wecom.domain.dto.common.Attachments;
import com.easyink.wecom.domain.dto.message.*;
import com.easyink.wecom.login.util.LoginTokenService;
import com.easyink.wecom.mapper.WeCustomerMessageMapper;
import com.easyink.wecom.service.*;
import com.easyink.wecom.utils.AttachmentService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 群发消息  微信消息表service接口
 *
 * @author admin
 * @date 2020-12-12
 */
@Service
@Slf4j
public class WeCustomerMessageServiceImpl extends ServiceImpl<WeCustomerMessageMapper, WeCustomerMessage> implements WeCustomerMessageService {
    private final WeCustomerMessageMapper weCustomerMessageMapper;
    private final WeCustomerMessagePushClient weCustomerMessagePushClient;
    private final WeMaterialService weMaterialService;
    private final WeCustomerMessgaeResultService weCustomerMessgaeResultService;
    private final RedisCache redisCache;
    private final WeMsgTlpMaterialService weMsgTlpMaterialService;
    private final AttachmentService attachmentService;

    @Autowired
    public WeCustomerMessageServiceImpl(WeCustomerMessageMapper weCustomerMessageMapper, WeCustomerMessagePushClient weCustomerMessagePushClient, WeMaterialService weMaterialService, WeCustomerMessgaeResultService weCustomerMessgaeResultService, RedisCache redisCache, WeMsgTlpMaterialService weMsgTlpMaterialService, AttachmentService attachmentService) {
        this.weCustomerMessageMapper = weCustomerMessageMapper;
        this.weCustomerMessagePushClient = weCustomerMessagePushClient;
        this.weMaterialService = weMaterialService;
        this.weCustomerMessgaeResultService = weCustomerMessgaeResultService;
        this.redisCache = redisCache;
        this.weMsgTlpMaterialService = weMsgTlpMaterialService;
        this.attachmentService = attachmentService;
    }


    @Override
    public int updateWeCustomerMessageActualSend(Long messageId, Integer actualSend) {
        return weCustomerMessageMapper.updateWeCustomerMessageActualSend(messageId, actualSend);
    }

    @Override
    public void saveWeCustomerMessage(LoginUser loginUser, long messageId, long messageOriginalId, CustomerMessagePushDTO customerMessagePushDTO, int size) {
        //保存微信消息
        //微信群发消息表 WeCustomerMessage
        WeCustomerMessage customerMessage = new WeCustomerMessage();
        customerMessage.setOriginalId(messageOriginalId);
        customerMessage.setMessageId(messageId);
        customerMessage.setChatType(customerMessagePushDTO.getPushType());
        customerMessage.setCheckStatus(WeConstans.sendMessageStatusEnum.NOT_SEND.getStatus());
        customerMessage.setDelFlag(WeConstans.WE_CUSTOMER_MSG_RESULT_NO_DEFALE);
        String content = StrUtil.EMPTY;
        if (StringUtils.isNotBlank(customerMessagePushDTO.getTextMessage().getContent())) {
            content = customerMessagePushDTO.getTextMessage().getContent();
        }
        customerMessage.setContent(content);
        if (loginUser.isSuperAdmin()) {
            customerMessage.setCreateBy(LoginTokenService.getUsername());
        } else {
            //企微用户保存userId
            customerMessage.setCreateBy(loginUser.getWeUser().getUserId());
        }
        customerMessage.setMsgid(StrUtil.EMPTY);
        if (StringUtils.isNotEmpty(customerMessagePushDTO.getSettingTime())) {
            //定时任务
            customerMessage.setSettingTime(customerMessagePushDTO.getSettingTime());
            customerMessage.setTimedTask(WeConstans.TIME_TASK);
        } else {
            //立即发送
            customerMessage.setTimedTask(WeConstans.NORMAL_TASK);
        }
        customerMessage.setExpectSend(size);
        customerMessage.setSender(Optional.ofNullable(customerMessagePushDTO.getStaffId()).orElse(StrUtil.EMPTY));
        this.save(customerMessage);
    }

    @Override
    public void updateMsgId(long messageId, List<String> msgIds) {
        //通过messageId更新msgIds
        WeCustomerMessage weCustomerMessage = new WeCustomerMessage();
        weCustomerMessage.setMessageId(messageId);
        weCustomerMessage.setMsgid(msgIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        weCustomerMessageMapper.updateWeCustomerMessageMsgIdById(weCustomerMessage);
    }

    /**
     * 发送消息(定时任务调度)
     *
     * @param customerMessagePushDTO 消息信息
     * @param messageId              消息id
     * @param customers              客户
     */
    @Override
    public void sendMessage(CustomerMessagePushDTO customerMessagePushDTO, Long messageId, List<WeCustomer> customers) {
        //校验corpId
        StringUtils.checkCorpId(customerMessagePushDTO.getCorpId());
        WeCustomerMessagePushDTO messagePushDto = new WeCustomerMessagePushDTO();
        List<String> msgid = new ArrayList<>();
        //发送类类型: 给单个客户发，群发
        //发给客户
        if (WeConstans.SEND_MESSAGE_CUSTOMER.equals(customerMessagePushDTO.getPushType())) {
            //设置发送类型
            messagePushDto.setChat_type(ChatType.of(customerMessagePushDTO.getPushType()).getName());
            singleSendMessage(customerMessagePushDTO, messagePushDto, messageId, msgid, customers);
        } else {
            //发给客户群
            messagePushDto.setChat_type(ChatType.of(customerMessagePushDTO.getPushType()).getName());
            //客户群的员工id 发给群的部分，多个员工
            String[] userIds = customerMessagePushDTO.getStaffId().split(WeConstans.COMMA);
            for (String userId : userIds) {
                messagePushDto.setSender(userId);
                childMessage(messagePushDto, customerMessagePushDTO);
                //调用企微接口发送
                sendMessage(messagePushDto, customerMessagePushDTO.getCorpId(), messageId, msgid);
            }
        }
        this.updateMsgId(messageId, msgid);
    }

    /**
     * 员工单独群发消息
     *
     * @param customerMessagePushDTO
     * @param messagePushDto
     * @param messageId
     * @param msgId
     * @param customers
     */
    @Async
    void singleSendMessage(CustomerMessagePushDTO customerMessagePushDTO, WeCustomerMessagePushDTO messagePushDto, Long messageId, List<String> msgId, List<WeCustomer> customers) {
        final Set<String> userIds = customers.stream().map(WeCustomer::getUserId).collect(Collectors.toSet());
        for (String userId : userIds) {
            messagePushDto.setSender(userId);
            //查找该员工对应的客户
            messagePushDto.setExternal_userid(SpringUtils.getBean(WeCustomerMessagePushService.class).getExternalUserIds(customerMessagePushDTO.getCorpId(),
                    customerMessagePushDTO.getPushRange(),
                    userId,
                    customerMessagePushDTO.getDepartment(),
                    customerMessagePushDTO.getTag(),
                    customerMessagePushDTO.getFilterTags(),
                    customerMessagePushDTO.getGender(),
                    customerMessagePushDTO.getCustomerStartTime(),
                    customerMessagePushDTO.getCustomerEndTime()).stream().map(WeCustomer::getExternalUserid).collect(Collectors.toList()));
            childMessage(messagePushDto, customerMessagePushDTO);
            //调用企微接口发送
            sendMessage(messagePushDto, customerMessagePushDTO.getCorpId(), messageId, msgId);
        }
    }

    @Override
    public int deleteByMessageId(Long messageId, String corpId) {
        StringUtils.checkCorpId(corpId);
        return weCustomerMessageMapper.deleteByMessageId(messageId, corpId);
    }

    /**
     * 调用企微接口发送
     *
     * @param messagePushDto 发送参数
     * @param corpId         企业id
     * @param messageId      消息id
     */
    private void sendMessage(WeCustomerMessagePushDTO messagePushDto, String corpId, Long messageId, List<String> msgid) {
        //调用企微接口捕获相应异常 设置发送结果
        try {
            SendMessageResultDTO sendMessageResultDTO = weCustomerMessagePushClient.sendCustomerMessageToUser(messagePushDto, corpId);
            msgid.add(sendMessageResultDTO.getMsgid());
        } catch (Exception e) {
            log.error("群发异常 messageId:{}, ex:{}", messageId, ExceptionUtils.getStackTrace(e));
            Integer errcode = JSONObject.parseObject(e.getMessage()).getInteger("errcode");
            LambdaUpdateWrapper<WeCustomerMessgaeResult> updateWrapper = new LambdaUpdateWrapper<WeCustomerMessgaeResult>()
                    .eq(WeCustomerMessgaeResult::getMessageId, messageId);
            if (WeExceptionTip.WE_EXCEPTION_TIP_41048.getCode().equals(errcode)) {
                weCustomerMessgaeResultService.update(updateWrapper.set(WeCustomerMessgaeResult::getStatus, MessageStatusEnum.MAX_TIMES.getType())
                        .set(WeCustomerMessgaeResult::getRemark, MessageStatusEnum.MAX_TIMES.getName()));
            } else if (WeExceptionTip.WE_EXCEPTION_TIP_90208.getCode().equals(errcode)) {
                weCustomerMessgaeResultService.update(updateWrapper.set(WeCustomerMessgaeResult::getStatus, MessageStatusEnum.MINI_PROGRAM_ERROR.getType())
                        .set(WeCustomerMessgaeResult::getRemark, MessageStatusEnum.MINI_PROGRAM_ERROR.getName()));
            } else if (WeExceptionTip.WE_EXCEPTION_TIP_40058.getCode().equals(errcode)) {
                weCustomerMessgaeResultService.update(updateWrapper.set(WeCustomerMessgaeResult::getStatus, MessageStatusEnum.PARAM_ERROR.getType())
                        .set(WeCustomerMessgaeResult::getRemark, MessageStatusEnum.PARAM_ERROR.getName()));
            }
        } finally {
            //尝试发送过则标记为已发送
            this.update(new LambdaUpdateWrapper<WeCustomerMessage>()
                    .eq(WeCustomerMessage::getMessageId, messageId)
                    .set(WeCustomerMessage::getCheckStatus, MessageStatusEnum.SEND_SUCCEED.getType()));
        }
    }


    /**
     * 子消息体
     *
     * @param weCustomerMessagePushDTO 群发消息体
     * @param customerMessagePushDTO   群发消息
     */
    private void childMessage(WeCustomerMessagePushDTO weCustomerMessagePushDTO, CustomerMessagePushDTO customerMessagePushDTO) {
        List<com.easyink.wecom.domain.dto.common.Attachment> attachmentList = new ArrayList();
        List<Attachment> attachments = customerMessagePushDTO.getAttachments();
        //数量超出上限抛异常
        if (attachments.size() > WeConstans.MAX_ATTACHMENT_NUM) {
            throw new CustomException(ResultTip.TIP_ATTACHMENT_OVER);
        }
        //处理文本
        if (customerMessagePushDTO.getTextMessage() != null) {
            weCustomerMessagePushDTO.setText(customerMessagePushDTO.getTextMessage().toText());
        }
        //处理附件
        if (CollectionUtils.isNotEmpty(attachments)) {
            attachments.forEach(attachment -> {
                AttachmentParam param = AttachmentParam.costFromAttachment(weCustomerMessagePushDTO.getSender(), customerMessagePushDTO.getCorpId(), customerMessagePushDTO.getTaskName(), attachment);
                Attachments attach = null;
                try {
                    attach = attachmentService.buildAttachment(param, customerMessagePushDTO.getCorpId());
                } catch (Exception e) {
                    LambdaUpdateWrapper<WeCustomerMessgaeResult> updateWrapper = new LambdaUpdateWrapper<WeCustomerMessgaeResult>()
                            .eq(WeCustomerMessgaeResult::getMessageId, customerMessagePushDTO.getMessageId())
                            .set(WeCustomerMessgaeResult::getStatus, MessageStatusEnum.MEDIA_ID_ERROR.getType())
                            .set(WeCustomerMessgaeResult::getRemark, MessageStatusEnum.MEDIA_ID_ERROR.getName());
                    weCustomerMessgaeResultService.update(updateWrapper);
                }
                if (attach != null) {
                    attachmentList.add(attach);
                }
//                attachmentHandler(attachment, customerMessagePushDTO.getCorpId(), attachmentList, customerMessagePushDTO.getMessageId());
            });
        }
        weCustomerMessagePushDTO.setAttachments(attachmentList);
    }

    /**
     * 处理不同类型的附件
     *
     * @param attachment     附件
     * @param corpId         企业id
     * @param attachmentList 附件列表（保存附件）
     */
    private void attachmentHandler(Attachment attachment, String corpId, List attachmentList, Long messageId) {
        //校验CorpId
        StringUtils.checkCorpId(corpId);
        JSONObject jsonObject = new JSONObject();
        String msgtype = attachment.getMsgtype();
        //图片
        if (GroupMessageType.IMAGE.getType().equals(msgtype)) {
            jsonObject.put(WeConstans.MSG_TYPE, GroupMessageType.IMAGE.getMessageType());
            ImageMessageDTO imageMessage = attachment.getImageMessage();
            String mdiaId = buildMediaId(imageMessage.getPic_url(),
                    GroupMessageType.IMAGE.getMessageType(), FileUtil.getName(imageMessage.getPic_url()), corpId, messageId);
            imageMessage.setMedia_id(mdiaId);
            jsonObject.put(GroupMessageType.IMAGE.getMessageType(), imageMessage);
        }
        //小程序
        else if (GroupMessageType.MINIPROGRAM.getType().equals(msgtype)) {
            jsonObject.put(WeConstans.MSG_TYPE, GroupMessageType.MINIPROGRAM.getMessageType());
            MiniprogramMessageDTO miniprogramMessage = attachment.getMiniprogramMessage();
            String mdiaId = buildMediaId(miniprogramMessage.getPicUrl(), GroupMessageType.IMAGE.getMessageType(), FileUtil.getName(miniprogramMessage.getPicUrl()), corpId, messageId);
            miniprogramMessage.setPic_media_id(mdiaId);
            jsonObject.put(GroupMessageType.MINIPROGRAM.getMessageType(), miniprogramMessage);
        }
        //链接
        else if (GroupMessageType.LINK.getType().equals(msgtype)) {
            jsonObject.put(WeConstans.MSG_TYPE, GroupMessageType.LINK.getMessageType());
            jsonObject.put(GroupMessageType.LINK.getMessageType(), attachment.getLinkMessage());
        } else if (GroupMessageType.RADAR.getType().equals(msgtype)) {
            jsonObject.put(WeConstans.MSG_TYPE, GroupMessageType.RADAR.getMessageType());
            jsonObject.put(GroupMessageType.RADAR.getMessageType(), attachment.getLinkMessage());
        }
        //视频
        else if (GroupMessageType.VIDEO.getType().equals(msgtype)) {
            VideoDTO videoDTO = attachment.getVideoDTO();
            //大于10M发送链接
            if (videoDTO.getSize() != null && videoDTO.getSize() > WeConstans.DEFAULT_MAX_VIDEO_SIZE) {
                buildLinkJson(jsonObject, videoDTO);
            } else {
                jsonObject.put(WeConstans.MSG_TYPE, GroupMessageType.VIDEO.getMessageType());
                String mdiaId = buildMediaId(videoDTO.getVideoUrl(), GroupMessageType.VIDEO.getMessageType(), FileUtil.getName(videoDTO.getVideoUrl()), corpId, messageId);
                videoDTO.setMedia_id(mdiaId);
                //上传失败转链接
                if (StringUtils.isBlank(mdiaId)) {
                    buildLinkJson(jsonObject, videoDTO);
                    //状态置为未执行
                    LambdaUpdateWrapper<WeCustomerMessgaeResult> updateWrapper = new LambdaUpdateWrapper<WeCustomerMessgaeResult>()
                            .eq(WeCustomerMessgaeResult::getMessageId, messageId)
                            .set(WeCustomerMessgaeResult::getStatus, MessageStatusEnum.NOT_SEND.getType())
                            .set(WeCustomerMessgaeResult::getRemark, MessageStatusEnum.NOT_SEND.getName());
                    weCustomerMessgaeResultService.update(updateWrapper);
                } else {
                    jsonObject.put(GroupMessageType.VIDEO.getMessageType(), videoDTO);
                }
            }
        }
        //文件
        else if (GroupMessageType.FILE.getType().equals(msgtype)) {
            jsonObject.put(WeConstans.MSG_TYPE, GroupMessageType.FILE.getMessageType());
            FileDTO fileDTO = attachment.getFileDTO();
            String mdiaId = buildMediaId(fileDTO.getFileUrl(), GroupMessageType.FILE.getMessageType(), FileUtil.getName(fileDTO.getFileUrl()), corpId, messageId);
            fileDTO.setMedia_id(mdiaId);
            jsonObject.put(GroupMessageType.FILE.getMessageType(), fileDTO);
        } else {
            //不存在附件直接返回
            return;
        }
        attachmentList.add(jsonObject);
    }

    /**
     * 构造链接json
     *
     * @param jsonObject json
     * @param videoDTO   视频dto
     */
    private void buildLinkJson(JSONObject jsonObject, VideoDTO videoDTO) {
        LinkMessageDTO linkMessageDTO = video2Link(videoDTO);
        jsonObject.put(WeConstans.MSG_TYPE, GroupMessageType.LINK.getMessageType());
        jsonObject.put(GroupMessageType.LINK.getMessageType(), linkMessageDTO);
    }

    /**
     * 视频转链接
     *
     * @param videoDTO 视频
     * @return {@link LinkMessageDTO}
     */
    private LinkMessageDTO video2Link(VideoDTO videoDTO) {
        LinkMessageDTO linkMessageDTO = new LinkMessageDTO();
        linkMessageDTO.setUrl(videoDTO.getVideoUrl());
        linkMessageDTO.setDesc("点击查看视频");
        //设置标题
        if (StringUtils.isNotBlank(videoDTO.getTitle())) {
            linkMessageDTO.setTitle(videoDTO.getTitle());
        } else {
            linkMessageDTO.setTitle(FileUtil.getName(videoDTO.getVideoUrl()));
        }
        //设置封面
        if (StringUtils.isNotBlank(videoDTO.getCoverUrl())) {
            linkMessageDTO.setPicurl(videoDTO.getCoverUrl());
        } else {
            linkMessageDTO.setPicurl(WeConstans.DEFAULT_VIDEO_COVER_URL);
        }
        return linkMessageDTO;
    }

    /**
     * 获得mediaId
     *
     * @param url       url
     * @param type      类型
     * @param name      名称
     * @param corpId    企业id
     * @param messageId 消息id
     * @return mediaId
     */
    private String buildMediaId(String url, String type, String name, String corpId, Long messageId) {
        Object cacheObject = redisCache.getCacheObject(WeConstans.MEDIA_KEY + url);
        String mediaId = StrUtil.EMPTY;
        if (cacheObject != null) {
            mediaId = cacheObject.toString();
            return mediaId;
        }
        try {
            WeMediaDTO weMediaDTO = weMaterialService.uploadTemporaryMaterial(url, type, name, corpId);
            mediaId = weMediaDTO.getMedia_id();
            redisCache.setCacheObject(WeConstans.MEDIA_KEY + url, weMediaDTO.getMedia_id(), 2, TimeUnit.DAYS);
        } catch (Exception e) {
            LambdaUpdateWrapper<WeCustomerMessgaeResult> updateWrapper = new LambdaUpdateWrapper<WeCustomerMessgaeResult>()
                    .eq(WeCustomerMessgaeResult::getMessageId, messageId)
                    .set(WeCustomerMessgaeResult::getStatus, MessageStatusEnum.MEDIA_ID_ERROR.getType())
                    .set(WeCustomerMessgaeResult::getRemark, MessageStatusEnum.MEDIA_ID_ERROR.getName());
            weCustomerMessgaeResultService.update(updateWrapper);
        }
        return mediaId;
    }

    /**
     * 获得朋友圈附件id（企业朋友圈使用）
     *
     * @param url    链接
     * @param type   类型
     * @param name   名称
     * @param corpId 企业id
     * @return 附件id
     */
    @Override
    public String buildMediaId(String url, String type, Integer attachmentType, String name, String corpId) {
        Object cacheObject = redisCache.getCacheObject(WeConstans.MOMENT_ATTACHMENT_MEDIA_KEY + url);
        String mediaId;
        if (cacheObject != null) {
            mediaId = cacheObject.toString();
            return mediaId;
        }
        WeMediaDTO weMediaDTO = weMaterialService.uploadAttachment(url, type, attachmentType, name, corpId);
        mediaId = weMediaDTO.getMedia_id();
        //缓存一小时
        redisCache.setCacheObject(WeConstans.MOMENT_ATTACHMENT_MEDIA_KEY + url, weMediaDTO.getMedia_id(), 1, TimeUnit.HOURS);
        return mediaId;
    }

    /**
     * 获得临时素材id（朋友圈客户端可使用）
     *
     * @param url    链接
     * @param type   类型
     * @param name   名称
     * @param corpId 企业id
     * @return 临时素材id
     */
    @Override
    public String buildMediaId(String url, String type, String name, String corpId) {
        Object cacheObject = redisCache.getCacheObject(WeConstans.MEDIA_KEY + url);
        String mediaId = StrUtil.EMPTY;
        if (cacheObject != null) {
            mediaId = cacheObject.toString();
            return mediaId;
        }
        try {
            //todo 素材校验
            WeMediaDTO weMediaDTO = weMaterialService.uploadTemporaryMaterial(url, type, name, corpId);
            if (weMediaDTO != null && StringUtils.isNotBlank(weMediaDTO.getMedia_id())) {
                mediaId = weMediaDTO.getMedia_id();
                redisCache.setCacheObject(WeConstans.MEDIA_KEY + url, weMediaDTO.getMedia_id(), 2, TimeUnit.DAYS);
            }
        } catch (Exception e) {
            log.error("附件临时素材上传失败 corpId:{},url:{},e:{}", corpId, url, ExceptionUtils.getStackTrace(e));
        }
        return mediaId;
    }
}
