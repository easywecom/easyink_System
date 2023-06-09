package com.easyink.wecom.service.impl.autotag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.easyink.common.core.domain.wecom.WeUser;
import com.easyink.common.enums.ResultTip;
import com.easyink.common.exception.CustomException;
import com.easyink.common.utils.DateUtils;
import com.easyink.common.utils.StringUtils;
import com.easyink.wecom.domain.WeTag;
import com.easyink.wecom.domain.entity.autotag.WeAutoTagRule;
import com.easyink.wecom.domain.entity.autotag.WeAutoTagRuleHitCustomerRecord;
import com.easyink.wecom.domain.entity.autotag.WeAutoTagRuleHitCustomerRecordTagRel;
import com.easyink.wecom.domain.query.autotag.CustomerTagRuleRecordQuery;
import com.easyink.wecom.domain.query.autotag.TagRuleQuery;
import com.easyink.wecom.domain.vo.autotag.TagRuleListVO;
import com.easyink.wecom.domain.vo.autotag.record.CustomerCountVO;
import com.easyink.wecom.domain.vo.autotag.record.customer.CustomerTagRuleRecordVO;
import com.easyink.wecom.mapper.autotag.WeAutoTagRuleHitCustomerRecordMapper;
import com.easyink.wecom.mapper.autotag.WeAutoTagRuleHitCustomerRecordTagRelMapper;
import com.easyink.wecom.mapper.autotag.WeAutoTagRuleMapper;
import com.easyink.wecom.service.WeCustomerService;
import com.easyink.wecom.service.WeCustomerTrajectoryService;
import com.easyink.wecom.service.WeUserService;
import com.easyink.wecom.service.autotag.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 客户打标签记录表(WeAutoTagRuleHitCustomerRecord)表服务实现类
 *
 * @author tigger
 * @since 2022-03-02 16:04:52
 */
@Slf4j
@Service("weAutoTagRuleHitCustomerRecordService")
public class WeAutoTagRuleHitCustomerRecordServiceImpl extends ServiceImpl<WeAutoTagRuleHitCustomerRecordMapper, WeAutoTagRuleHitCustomerRecord> implements WeAutoTagRuleHitCustomerRecordService {
    @Autowired
    private WeCustomerService weCustomerService;
    @Autowired
    private WeAutoTagRuleService weAutoTagRuleService;

    @Autowired
    private WeAutoTagUserRelService weAutoTagUserRelService;

    @Autowired
    private WeAutoTagCustomerSceneService weAutoTagCustomerSceneService;
    @Autowired
    private WeAutoTagCustomerSceneTagRelService weAutoTagCustomerSceneTagRelService;
    @Autowired
    private WeAutoTagRuleHitCustomerRecordTagRelService weAutoTagRuleHitCustomerRecordTagRelService;
    @Autowired
    private WeAutoTagRuleHitCustomerRecordTagRelMapper weAutoTagRuleHitCustomerRecordTagRelMapper;
    @Autowired
    private WeCustomerTrajectoryService weCustomerTrajectoryService;
    @Autowired
    private WeAutoTagRuleMapper weAutoTagRuleMapper;

    private final WeUserService weUserService;

    @Autowired
    public WeAutoTagRuleHitCustomerRecordServiceImpl(WeUserService weUserService) {
        this.weUserService = weUserService;
    }

    /**
     * 新客规则记录列表
     *
     * @param query
     * @return
     */
    @Override
    public List<CustomerTagRuleRecordVO> listCustomerRecord(CustomerTagRuleRecordQuery query) {
        // 处理结束之间为endDay
        if (StringUtils.isNotBlank(query.getBeginTime()) && StringUtils.isNotBlank(query.getEndTime())) {
            if (!DateUtils.isMatchFormat(query.getBeginTime(), DateUtils.YYYY_MM_DD)) {
                throw new CustomException(ResultTip.TIP_TIME_FORMAT_ERROR);
            }
            query.setBeginTime(DateUtils.parseBeginDay(query.getBeginTime()));
            if (DateUtils.isMatchFormat(query.getBeginTime(), DateUtils.YYYY_MM_DD)) {
                throw new CustomException(ResultTip.TIP_TIME_FORMAT_ERROR);
            }
            query.setEndTime(DateUtils.parseEndDay(query.getEndTime()));
        }
        return this.baseMapper.listCustomerRecord(query);
    }

    /**
     * 新客户打标签
     *
     * @param customerId 客户id
     * @param userId     员工id
     * @param corpId     企业id
     */
    @Override
    public void makeTagToNewCustomer(String customerId, String userId, String corpId) {
        if (StringUtils.isBlank(customerId) || StringUtils.isBlank(userId) || StringUtils.isBlank(corpId)) {
            throw new CustomException(ResultTip.TIP_GENERAL_BAD_REQUEST);
        }
        // 1. 查询符合的规则列表,1) labelType = 3 2) 存在生效周期,则判断是否在有效期内
        List<Long> candidatesRuleIdList = weAutoTagRuleService.getCandidateCustomerRuleIdList(corpId);
        if (CollectionUtils.isEmpty(candidatesRuleIdList)) {
            log.info("新客打标签, 无可用或有效期内的标签");
            return;
        }

        // 2.判断员工是否符合规则设置的使用人员范围,符合条件的添加到正式规则列表rules
        List<Long> availableRuleIdList = weAutoTagUserRelService.
                getCurrentUserIdAvailableCustomerRuleIdList(corpId, candidatesRuleIdList, userId);
        if (CollectionUtils.isEmpty(availableRuleIdList)) {
            log.info("新客打标签, 当前员工没有可使用范围内的标签规则, userId: {}", userId);
            return;
        }
        // 3.判断每个规则下的场景是否符合,符合的添加到正式的场景列表scenes
        Map<Long, List<Long>> availableSceneIdListGroupByRuleIdMap = weAutoTagCustomerSceneService.
                getAvailableSceneIdListFromRuleIdListGroupByRuleId(availableRuleIdList);
        List<WeAutoTagRuleHitCustomerRecord> batchAddRecordList = new ArrayList<>();
        List<WeAutoTagRuleHitCustomerRecordTagRel> batchAddTagRelList = new ArrayList<>();
        List<String> allTagIdList = new ArrayList<>();
        List<TagRuleListVO> ruleNameList = new ArrayList<>();
        Date date = new Date();
        for (Map.Entry<Long, List<Long>> sceneEntry : availableSceneIdListGroupByRuleIdMap.entrySet()) {
            Long ruleId = sceneEntry.getKey();
            List<Long> sceneIdList = sceneEntry.getValue();
            //查询规则名
            TagRuleQuery tagRuleQuery =new TagRuleQuery();
            tagRuleQuery.setTagIdList(sceneIdList.stream().map(String::valueOf).collect(Collectors.toList()));
            tagRuleQuery.setCorpId(corpId);
            List<TagRuleListVO> sceneRuleName = weAutoTagRuleMapper.listCustomer(tagRuleQuery);
            // 组装记录数据
            batchAddRecordList.add(new WeAutoTagRuleHitCustomerRecord(ruleId, corpId, customerId, userId, date));
            // 组装标签记录数据
            List<WeTag> tagList = weAutoTagCustomerSceneTagRelService.getTagListBySceneIdList(sceneIdList);
            allTagIdList.addAll(tagList.stream().map(WeTag::getTagId).collect(Collectors.toList()));
            List<WeAutoTagRuleHitCustomerRecordTagRel> tagRelList = weAutoTagRuleHitCustomerRecordTagRelService.buildTagRecord(ruleId, customerId, userId, tagList);
            batchAddTagRelList.addAll(tagRelList);
            ruleNameList.addAll(sceneRuleName);
        }
        // 4.添加记录和标签记录
        // 添加数据
        if (CollectionUtils.isNotEmpty(batchAddRecordList)) {
            this.baseMapper.insertOrUpdateBatch(batchAddRecordList);
        }
        if (CollectionUtils.isNotEmpty(batchAddTagRelList)) {
            weAutoTagRuleHitCustomerRecordTagRelMapper.insertBatch(batchAddTagRelList);
        }

        // 获取员工详情
        WeUser weUser = weUserService.getUserDetail(corpId, userId);
        if (weUser == null) {
            log.info("[新客打标签] 获取员工详情,查询不到员工信息,corpId:{},userId:{}", corpId, userId);
            return;
        }

        // 5.调用接口打标签
        if (CollectionUtils.isNotEmpty(allTagIdList)) {
            log.info(">>>>>>>>>>>>>>>准备进行打标签,标签列表: {}", allTagIdList);
            weCustomerService.singleMarkLabel(corpId, userId, customerId, allTagIdList, weUser.getName());
            weCustomerTrajectoryService.recordAutoCustomerTag(corpId,userId,customerId,ruleNameList);
        }


    }

    /**
     * 新客客户统计
     *
     * @param ruleId
     * @param corpId 企业id
     * @return
     */
    @Override
    public CustomerCountVO customerCustomerCount(Long ruleId, String corpId) {
        List<String> customerIdList = this.baseMapper.customerCustomerCount(ruleId, corpId);
        return new CustomerCountVO(customerIdList.size(), (int) customerIdList.stream().distinct().count());
    }
}

