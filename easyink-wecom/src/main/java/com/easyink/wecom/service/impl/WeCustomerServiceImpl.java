package com.easyink.wecom.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dtflys.forest.exceptions.ForestRuntimeException;
import com.easyink.common.annotation.DataScope;
import com.easyink.common.constant.Constants;
import com.easyink.common.constant.GenConstants;
import com.easyink.common.constant.WeConstans;
import com.easyink.common.core.domain.AjaxResult;
import com.easyink.common.core.domain.wecom.BaseExtendPropertyRel;
import com.easyink.common.core.domain.wecom.WeUser;
import com.easyink.common.enums.CustomerExtendPropertyEnum;
import com.easyink.common.enums.MethodParamType;
import com.easyink.common.enums.ResultTip;
import com.easyink.common.enums.customer.SubjectTypeEnum;
import com.easyink.common.enums.wecom.ServerTypeEnum;
import com.easyink.common.exception.CustomException;
import com.easyink.common.exception.wecom.WeComException;
import com.easyink.common.utils.DateUtils;
import com.easyink.common.utils.bean.BeanUtils;
import com.easyink.common.utils.poi.ExcelUtil;
import com.easyink.common.utils.sql.BatchInsertUtil;
import com.easyink.common.utils.wecom.CorpSecretDecryptUtil;
import com.easyink.wecom.annotation.Convert2Cipher;
import com.easyink.wecom.client.WeCustomerClient;
import com.easyink.wecom.client.WeUnionIdClient;
import com.easyink.wecom.client.WeUpdateIDClient;
import com.easyink.wecom.client.WeUserClient;
import com.easyink.wecom.domain.*;
import com.easyink.wecom.domain.dto.*;
import com.easyink.wecom.domain.dto.customer.CustomerTagEdit;
import com.easyink.wecom.domain.dto.customer.EditCustomerDTO;
import com.easyink.wecom.domain.dto.customer.ExternalUserDetail;
import com.easyink.wecom.domain.dto.customer.GetExternalDetailResp;
import com.easyink.wecom.domain.dto.customer.req.GetByUserReq;
import com.easyink.wecom.domain.dto.customer.resp.GetByUserResp;
import com.easyink.wecom.domain.dto.customersop.Column;
import com.easyink.wecom.domain.dto.pro.EditCustomerFromPlusDTO;
import com.easyink.wecom.domain.dto.tag.RemoveWeCustomerTagDTO;
import com.easyink.wecom.domain.dto.unionid.GetUnionIdDTO;
import com.easyink.wecom.domain.entity.WeCustomerExportDTO;
import com.easyink.wecom.domain.entity.WeExternalUseridMapping;
import com.easyink.wecom.domain.vo.QueryCustomerFromPlusVO;
import com.easyink.wecom.domain.vo.WeCustomerExportVO;
import com.easyink.wecom.domain.vo.WeMakeCustomerTagVO;
import com.easyink.wecom.domain.vo.customer.WeCustomerSumVO;
import com.easyink.wecom.domain.vo.customer.WeCustomerUserListVO;
import com.easyink.wecom.domain.vo.customer.WeCustomerVO;
import com.easyink.wecom.domain.vo.sop.CustomerSopVO;
import com.easyink.wecom.domain.vo.unionid.GetUnionIdVO;
import com.easyink.wecom.login.util.LoginTokenService;
import com.easyink.wecom.mapper.WeCustomerMapper;
import com.easyink.wecom.mapper.WeExternalUseridMappingMapper;
import com.easyink.wecom.service.*;
import com.easyink.wecom.service.wechatopen.WechatOpenService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotBlank;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 类名： WeCustomerServiceImpl
 *
 * @author 佚名
 * @date 2021/8/26 20:28
 */
@Slf4j
@Service
public class WeCustomerServiceImpl extends ServiceImpl<WeCustomerMapper, WeCustomer> implements WeCustomerService {
    @Autowired
    private WeCustomerMapper weCustomerMapper;
    @Autowired
    private WeCustomerClient weCustomerClient;
    @Autowired
    private WeFlowerCustomerRelService weFlowerCustomerRelService;

    @Autowired
    private WeTagGroupService weTagGroupService;

    @Autowired
    private WeUserService weUserService;

    @Autowired
    private WechatOpenService wechatOpenService;

    @Autowired
    @Lazy
    private WeFlowerCustomerTagRelService weFlowerCustomerTagRelService;

    @Autowired
    private WeUserClient weUserClient;
    @Autowired
    private WeCustomerTrajectoryService weCustomerTrajectoryService;
    @Autowired
    @Lazy
    private PageHomeService pageHomeService;
    @Autowired
    private WeCustomerExtendPropertyRelService weCustomerExtendPropertyRelService;
    @Autowired
    @Lazy
    private WeCustomerExtendPropertyService weCustomerExtendPropertyService;
    @Autowired
    private WeUnionIdClient weUnionIdClient ;
    @Autowired
    private CorpSecretDecryptUtil corpSecretDecryptUtil ;

    @Autowired
    private We3rdAppService we3rdAppService;

    @Autowired
    private WeExternalUseridMappingMapper weExternalUseridMappingMapper;

    @Autowired
    private WeUpdateIDClient convertIDClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdate(WeCustomer weCustomer) {
        if (weCustomer == null) {
            return false;
        }
        if (StringUtils.isNotBlank(weCustomer.getExternalUserid())
                && StringUtils.isNotBlank(weCustomer.getUserId())
                && StringUtils.isNotBlank(weCustomer.getCorpId())) {
            //添加corpId不为空
            WeCustomer weCustomerBean = selectWeCustomerById(weCustomer.getExternalUserid(), weCustomer.getCorpId());
            if (weCustomerBean != null) {
                WeFlowerCustomerRel weFlowerCustomerRel = new WeFlowerCustomerRel();
                weFlowerCustomerRel.setCorpId(weCustomer.getCorpId());
                weFlowerCustomerRel.setRemark(weCustomer.getRemark());
                weFlowerCustomerRel.setUserId(weCustomer.getUserId());
                weFlowerCustomerRel.setRemarkMobiles(weCustomer.getPhone());
                weFlowerCustomerRel.setDescription(weCustomer.getDesc());

                weFlowerCustomerRelService.update(weFlowerCustomerRel, new LambdaQueryWrapper<WeFlowerCustomerRel>()
                        .eq(WeFlowerCustomerRel::getExternalUserid, weCustomer.getExternalUserid())
                        .eq(WeFlowerCustomerRel::getUserId, weCustomer.getUserId())
                        .eq(WeFlowerCustomerRel::getCorpId, weCustomer.getCorpId()));

                return weCustomerMapper.updateWeCustomer(weCustomer) == 1;
            } else {
                return weCustomerMapper.insertWeCustomer(weCustomer) == 1;
            }
        }
        return false;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    @Convert2Cipher(paramType = MethodParamType.STRUCT)
    public void editCustomer(EditCustomerDTO dto) {
        if (dto == null || StringUtils.isAnyBlank(dto.getExternalUserid(), dto.getUserId(), dto.getCorpId())) {
            throw new CustomException(ResultTip.TIP_GENERAL_PARAM_ERROR);
        }
        String corpId = dto.getCorpId();
        String userId = dto.getUserId();
        String externalUserId = dto.getExternalUserid();
        // 1.修改客户生日
        WeCustomer weCustomer = dto.transferToCustomer();
        if (dto.getBirthday() != null) {
            weCustomerMapper.updateBirthday(weCustomer);
        }
        // 2.修改自定义扩展字段属性
        if (CollectionUtils.isNotEmpty(weCustomer.getExtendProperties())) {
            weCustomerExtendPropertyRelService.updateBatch(weCustomer);
        }

        // 3.修改跟进人对客户的备注信息
        WeFlowerCustomerRel rel = dto.transferToCustomerRel();
        WeCustomerDTO.WeCustomerRemark editReq = new WeCustomerDTO().new WeCustomerRemark(rel);
        weCustomerClient.remark(editReq, corpId);
        weFlowerCustomerRelService.update(rel, new LambdaUpdateWrapper<WeFlowerCustomerRel>()
                .eq(WeFlowerCustomerRel::getCorpId, corpId)
                .eq(WeFlowerCustomerRel::getExternalUserid, externalUserId)
                .eq(WeFlowerCustomerRel::getUserId, userId)
        );
        // 4.修改客户标签
        if (CollUtil.isNotEmpty(dto.getAddTags()) || CollUtil.isNotEmpty(dto.getRemoveTags())) {
            // 批量修改标签
            WeCustomerService weCustomerService = (WeCustomerServiceImpl)AopContext.currentProxy();
            weCustomerService.batchMarkCustomTag(corpId, userId, externalUserId, dto.getAddTags(), dto.getRemoveTags());
        }
        // 5.添加轨迹内容(信息动态)
        weCustomerTrajectoryService.recordEditOperation(dto);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void batchMarkCustomTagWithTagIds(String corpId, String userId, String externalUserId, List<String> addTagIds, List<String> removeTagIds) {
        if (StringUtils.isAnyBlank(userId, externalUserId, corpId) || (CollUtil.isEmpty(addTagIds) && CollUtil.isEmpty(removeTagIds))) {
            log.info("员工id，客户id，公司id不能为空，userId：{}，externalUserId：{}，corpId：{}", userId, externalUserId, corpId);
            throw new CustomException("增量式打标签错误");
        }
        if (CollUtil.isEmpty(addTagIds) && CollUtil.isEmpty(removeTagIds)) {
            return;
        }
        //查询查出员工和客户关系
        WeFlowerCustomerRel flowerCustomerRel = weFlowerCustomerRelService.getOne(userId, externalUserId, corpId);
        if (flowerCustomerRel == null) {
            return;
        }
        if (CollUtil.isEmpty(addTagIds) && CollUtil.isEmpty(removeTagIds)) {
            return;
        }
        List<WeFlowerCustomerTagRel> addTagRels = new ArrayList<>();
        for (String tagId : addTagIds) {
            //本地标签关系
            addTagRels.add(
                    WeFlowerCustomerTagRel.builder()
                            .flowerCustomerRelId(flowerCustomerRel.getId())
                            .externalUserid(flowerCustomerRel.getExternalUserid())
                            .tagId(tagId)
                            .createTime(new Date())
                            .build()
            );
        }
        //官方接口参数
        log.info("可打的标签id列表: {}", addTagIds);
        log.info("可移除的标签id列表: {}", removeTagIds);
        CustomerTagEdit customerTagEdit = CustomerTagEdit.builder()
                .userid(flowerCustomerRel.getUserId())
                .external_userid(flowerCustomerRel.getExternalUserid())
                .build();
        // 新增标签
        if (CollUtil.isNotEmpty(addTagRels)) {
            weFlowerCustomerTagRelService.batchInsetWeFlowerCustomerTagRel(addTagRels);
            customerTagEdit.setAdd_tag(ArrayUtil.toArray(addTagIds, String.class));
        }
        // 删除标签
        if (CollUtil.isNotEmpty(removeTagIds)) {
            weFlowerCustomerTagRelService.remove(new LambdaQueryWrapper<WeFlowerCustomerTagRel>()
                    .eq(WeFlowerCustomerTagRel::getFlowerCustomerRelId, flowerCustomerRel.getId())
                    .eq(WeFlowerCustomerTagRel::getExternalUserid, flowerCustomerRel.getExternalUserid())
                    .in(WeFlowerCustomerTagRel::getTagId, removeTagIds));
            customerTagEdit.setRemove_tag(ArrayUtil.toArray(removeTagIds, String.class));
        }
        //企微新接口
        weCustomerClient.makeCustomerLabel(customerTagEdit, corpId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void batchMarkCustomTag(String corpId, String userId, String externalUserId, List<WeTag> addTags, List<WeTag> removeTags) {
        if (StringUtils.isAnyBlank(corpId, userId, externalUserId) || (CollUtil.isEmpty(addTags) && CollUtil.isEmpty(removeTags))) {
            log.info("员工id，客户id，公司id不能为空，userId：{}，externalUserId：{}，corpId：{}", userId, externalUserId, corpId);
            throw new CustomException("增量式打标签错误");
        }
        List<String> addTagIds = addTags == null ? Collections.emptyList() : addTags.stream().map(WeTag::getTagId).filter(Objects::nonNull).collect(Collectors.toList());
        List<String> removeTagIds = removeTags == null ? Collections.emptyList() : removeTags.stream().map(WeTag::getTagId).filter(Objects::nonNull).collect(Collectors.toList());
        WeCustomerService weCustomerService = (WeCustomerServiceImpl)AopContext.currentProxy();
        weCustomerService.batchMarkCustomTagWithTagIds(corpId, userId, externalUserId, addTagIds, removeTagIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWeCustomerRemark(WeCustomer weCustomer) {
        if (weCustomer == null
                || StringUtils.isAnyBlank(weCustomer.getCorpId(), weCustomer.getUserId(), weCustomer.getExternalUserid())) {
            throw new CustomException(ResultTip.TIP_GENERAL_PARAM_ERROR);
        }
        // 自定义字段修改
        weCustomerExtendPropertyRelService.updateBatch(weCustomer);
        // 企微官方字段修改:如果为“”代表是清除描述的状态，也不能过滤，所以只过滤为空的状态
        if (weCustomer.getRemark() != null || weCustomer.getPhone() != null || weCustomer.getDesc() != null) {
            //如果有修改到备注和描述，调用企微接口修改企微数据
            WeCustomerDTO.WeCustomerRemark weCustomerRemark = new WeCustomerDTO().new WeCustomerRemark(weCustomer);
            //使用企微新接口
            weCustomerClient.remark(weCustomerRemark, weCustomer.getCorpId());
        }
        saveOrUpdate(weCustomer);
    }

    /**
     * 查询企业微信客户
     *
     * @param externalUserId 企业微信客户ID
     * @return 企业微信客户
     */
    @Override
    public WeCustomer selectWeCustomerById(String externalUserId, String corpId) {
        if (StringUtils.isAnyBlank(externalUserId, corpId)) {
            log.error("查询企业数据：externalUserId = {} , corpId = {}", externalUserId, corpId);
            throw new CustomException("查询企业数据失败");
        }

        return weCustomerMapper.selectWeCustomerById(externalUserId, corpId);
    }

    /**
     * 获取用户列表
     *
     * @param weCustomer {@link WeCustomer}
     * @return
     */
    @Override
    @DataScope
    public List<WeCustomerVO> selectWeCustomerListV2(WeCustomer weCustomer) {


        if (StringUtils.isBlank(weCustomer.getCorpId())) {
            throw new CustomException(ResultTip.TIP_GENERAL_PARAM_ERROR);
        }
        if (CollectionUtils.isNotEmpty(weCustomer.getExtendProperties())){
            List<WeCustomerExtend> extendList=extendProperties(weCustomer);
            if (CollectionUtils.isEmpty(extendList)){
                return new ArrayList<>();
            }
            weCustomer.setExtendList(extendList);
        }

         return weCustomerMapper.selectWeCustomerListV2(weCustomer);
    }

    /**
     * 处理用户自定义字段
     *
     * @param weCustomer
     * @return 筛选后的检索列表
     */
    public  List<WeCustomerExtend> extendProperties(WeCustomer weCustomer) {
        List<BaseExtendPropertyRel> baseExtendPropertyRelList = weCustomer.getExtendProperties();
        List<BaseExtendPropertyRel> oneLineType = new ArrayList<>();
        List<BaseExtendPropertyRel> multipleType = new ArrayList<>();
        List<BaseExtendPropertyRel> timeType = new ArrayList<>();

        for (BaseExtendPropertyRel baseExtendPropertyRel : baseExtendPropertyRelList) {
            baseExtendPropertyRel.setCorpId(weCustomer.getCorpId());
            // 判断是否为单行文本类型
            if (Objects.equals(baseExtendPropertyRel.getPropertyType(), CustomerExtendPropertyEnum.SINGLE_ROW.getType())) {
                oneLineType.add(baseExtendPropertyRel);
            }
            //判断是否为下拉框类型
            else if (Objects.equals(baseExtendPropertyRel.getPropertyType(), CustomerExtendPropertyEnum.COMBO_BOX.getType())) {
                multipleType.add(baseExtendPropertyRel);
            }
            //判断是否为时间类型
            else if (Objects.equals(baseExtendPropertyRel.getPropertyType(), CustomerExtendPropertyEnum.DATE.getType())) {
                timeType.add(baseExtendPropertyRel);
            }
        }

        List<BaseExtendPropertyRel> oneLineList =  new ArrayList<>();

        if (oneLineType.size()>0){
            oneLineList=weCustomerMapper.selectTypeOneLine(oneLineType);
            if (oneLineList.size()==0){
                return new ArrayList<>();
            }
        }
        if (multipleType.size()>0){
            List<BaseExtendPropertyRel> multipleList=weCustomerMapper.selectTypeMultiple(multipleType);
            if (multipleList.size()==0){
                return new ArrayList<>();
            }
            oneLineList=getIntersectionTypeNotFind(oneLineList,multipleList);
            if (oneLineList.size()==0){
                return new ArrayList<>();
            }
        }
        if (timeType.size()>0){
            List<BaseExtendPropertyRel> timeList=weCustomerMapper.selectTypeTime(timeType);
            if (timeList.size()==0){
                return new ArrayList<>();
            }
            oneLineList=getIntersectionTypeNotFind(oneLineList,timeList);
        }

        List<WeCustomerExtend> extendList = new ArrayList<>();
        if (oneLineList.size() > 0) {
            for (BaseExtendPropertyRel list : oneLineList) {
                WeCustomerExtend weCustomerExtend = new WeCustomerExtend();
                weCustomerExtend.setExternalUserid(list.getExternalUserid());
                weCustomerExtend.setUserId(list.getUserId());
                extendList.add(weCustomerExtend);
            }
        }
        return extendList;
    }


    /**
     * 获取查询所需集合。如果其中一个为空，返回另一个
     *
     * @param list1 集合1
     * @param list2 集合2
     * @return 查询所需的集合
     */
    public static List<BaseExtendPropertyRel> getIntersectionTypeNotFind(List<BaseExtendPropertyRel> list1,List<BaseExtendPropertyRel> list2) {
        if (list1.size()>0){
            if (list2.size()>0){
                list1.retainAll(list2);
                return list1;
            }else return list1;
        }
        return list2;
    }

    /**
     * 将DTO类转换为实体类
     *
     * @param weCustomerSearchDTO
     * @return
     */
    @Override
    public WeCustomer changeWecustomer(WeCustomerSearchDTO weCustomerSearchDTO){
        //初始化添加列表
        WeFlowerCustomerRel weFlowerCustomerRel =new WeFlowerCustomerRel();
        if (weCustomerSearchDTO.getBeginTime()!=null){
            weFlowerCustomerRel.setBeginTime(weCustomerSearchDTO.getBeginTime().toString());
        }
        if (weCustomerSearchDTO.getEndTime()!=null){
            weFlowerCustomerRel.setEndTime(weCustomerSearchDTO.getEndTime().toString());
        }
        if (!StringUtils.isBlank(weCustomerSearchDTO.getAddWay())){
            weFlowerCustomerRel.setAddWay(weCustomerSearchDTO.getAddWay());
        }
        if (!StringUtils.isBlank(weCustomerSearchDTO.getEmail())){
            weFlowerCustomerRel.setEmail(weCustomerSearchDTO.getEmail());
        }
        if (!StringUtils.isBlank(weCustomerSearchDTO.getAddress())){
            weFlowerCustomerRel.setAddress(weCustomerSearchDTO.getAddress());
        }
        List<WeFlowerCustomerRel> weFlowerCustomerRels = new ArrayList<>() ;
        weFlowerCustomerRels.add(weFlowerCustomerRel);
        // 转化实体类
        WeCustomer weCustomer =new WeCustomer();
        weCustomer.setCorpId(LoginTokenService.getLoginUser().getCorpId());
        weCustomer.setExternalUserid(weCustomerSearchDTO.getExternalUserid());
        weCustomer.setStatus(weCustomerSearchDTO.getStatus());
        weCustomer.setWeFlowerCustomerRels(weFlowerCustomerRels);

        if (!StringUtils.isBlank(weCustomerSearchDTO.getName())){
            weCustomer.setName(weCustomerSearchDTO.getName());
            weCustomer.setRemark(weCustomerSearchDTO.getName());
        }
        if (!StringUtils.isBlank(weCustomerSearchDTO.getDesc())){
            weCustomer.setDesc(weCustomerSearchDTO.getDesc());
        }
        if (!StringUtils.isBlank(weCustomerSearchDTO.getUserId())){
            weCustomer.setUserId(weCustomerSearchDTO.getUserId());
        }
        if (!StringUtils.isBlank(weCustomerSearchDTO.getUserIds())){
            weCustomer.setUserIds(weCustomerSearchDTO.getUserIds());
        }
        if (!StringUtils.isBlank(weCustomerSearchDTO.getTagIds())){
            weCustomer.setTagIds(weCustomerSearchDTO.getTagIds());
        }
        if (!StringUtils.isBlank(weCustomerSearchDTO.getGender())){
            weCustomer.setGender(weCustomerSearchDTO.getGender());
        }
        if (!StringUtils.isBlank(weCustomerSearchDTO.getCorpFullName())){
            weCustomer.setCorpFullName(weCustomerSearchDTO.getCorpFullName());
        }
        if (!StringUtils.isBlank(weCustomerSearchDTO.getBirthday())){
            weCustomer.setBirthdayStr(weCustomerSearchDTO.getBirthday());
        }
        if (!StringUtils.isBlank(weCustomerSearchDTO.getPhone())){
            weCustomer.setPhone(weCustomerSearchDTO.getPhone());
        }
        if (!StringUtils.isBlank(weCustomerSearchDTO.getBeginTime())){
            weCustomer.setBeginTime(weCustomerSearchDTO.getBeginTime());
        }
        if (!StringUtils.isBlank(weCustomerSearchDTO.getEndTime())){
            weCustomer.setEndTime(weCustomerSearchDTO.getEndTime());
        }
        if (weCustomerSearchDTO.getExtendProperties().size()>0){
            weCustomer.setExtendProperties(weCustomerSearchDTO.getExtendProperties());
        }

        return weCustomer;
    }

    @Override
    public List<WeCustomer> listOfUseCustomer(String corpId, WeOperationsCenterCustomerSopFilterEntity sopFilter) {
        WeCustomerPushMessageDTO weCustomerPushMessageDTO = new WeCustomerPushMessageDTO();
        weCustomerPushMessageDTO.setCorpId(corpId);
        //-1表示筛选条件未选择性别 选择性别的情况下添加过滤条件
        if (!Integer.valueOf(-1).equals(sopFilter.getGender())) {
            weCustomerPushMessageDTO.setGender(String.valueOf(sopFilter.getGender()));
        }
        weCustomerPushMessageDTO.setTagIds(sopFilter.getTagId());
        weCustomerPushMessageDTO.setCustomerStartTime(sopFilter.getStartTime());
        weCustomerPushMessageDTO.setCustomerEndTime(sopFilter.getEndTime());
        weCustomerPushMessageDTO.setUserIds(sopFilter.getUsers());
        weCustomerPushMessageDTO.setDepartmentIds(sopFilter.getDepartments());
        weCustomerPushMessageDTO.setFilterTags(sopFilter.getFilterTagId());
        //普通属性查询客户
        List<WeCustomer> weCustomers = selectWeCustomerListNoRel(weCustomerPushMessageDTO);
        if (StringUtils.isNotEmpty(sopFilter.getCloumnInfo())) {
            List<Column> columns = JSONArray.parseArray(sopFilter.getCloumnInfo(), Column.class);
            if (CollectionUtils.isNotEmpty(columns)) {
                //自定义属性查询客户
                List<String> customers = weCustomerExtendPropertyRelService.listOfPropertyIdAndValue(columns);
                //求两个集合交集
                weCustomers = weCustomers.stream().filter(customer -> customers.contains(customer.getExternalUserid())).collect(Collectors.toList());
            }
        }
        return weCustomers;
    }

    /**
     * 查询去重客户去重后企业微信客户列表
     *
     * @param weCustomer {@link WeCustomer}
     * @return
     */
    @DataScope
    @Override
    public List<WeCustomerVO> selectWeCustomerListDistinct(WeCustomer weCustomer) {
        if (StringUtils.isBlank(weCustomer.getCorpId())) {
            throw new CustomException(ResultTip.TIP_GENERAL_PARAM_ERROR);
        }
        return weCustomerMapper.selectWeCustomerListDistinct(weCustomer);
    }

    @Override
    public List<WeCustomerUserListVO> listUserListByCustomerId(String customerId, String corpId) {
        if (StringUtils.isAnyBlank(customerId, corpId)) {
            throw new CustomException(ResultTip.TIP_GENERAL_PARAM_ERROR);
        }
        return weCustomerMapper.selectUserListByCustomerId(customerId, corpId);
    }

    @Override
    @DataScope
    public WeCustomerSumVO weCustomerCount(WeCustomer weCustomer) {
        if (StringUtils.isAnyBlank(weCustomer.getCorpId())) {
            throw new CustomException(ResultTip.TIP_GENERAL_PARAM_ERROR);
        }
        List<WeCustomerVO> list = this.selectWeCustomerListV2(weCustomer);
        // 根据externalUserId去重
        Set<String> set = list.stream().map(WeCustomerVO::getExternalUserid).collect(Collectors.toSet());
        return WeCustomerSumVO.builder()
                .totalCount(list.size())
                .ignoreDuplicateCount(set.size())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Async
    public void syncWeCustomerV2(String corpId) {
        if (StringUtils.isBlank(corpId)) {
            throw new CustomException(ResultTip.TIP_MISS_CORP_ID);
        }
        log.info("开始同步客户,corpId:{}", corpId);
        long startTime = System.currentTimeMillis();

        // 1.调用[获取配置了客户联系功能的成员列表] 企微API
        List<String> userIdList = weCustomerClient.getFollowUserList(corpId).getFollowerUserIdList();

        // 2.每个员工依次调用[批量获取客户详情] 企微API, 同步其客户详情
        for (String userId : userIdList) {
            batchGetCustomerDetailAndSyncLocal(corpId, userId);
        }
        long endTime = System.currentTimeMillis();
        log.info("同步客户结束[新]:corpId:{}耗时：{}", corpId, Double.valueOf((endTime - startTime) / 1000.00D));
        // 3.客户信息同步后,刷新数据概览页客户数据
        pageHomeService.getCustomerData(corpId);

    }


    /**
     * 批量获取客户详情 并同步到本地
     *
     * @param corpId 企业id
     * @param userId 用户id
     */
    public void batchGetCustomerDetailAndSyncLocal(String corpId, String userId) {
        if (StringUtils.isBlank(corpId) || StringUtils.isBlank(userId)) {
            log.info("批量获取客户详情:参数缺失,corpId:{},userId:{}", corpId, userId);
            return;
        }
        // 1. API请求:请求企微[批量获取客户详情]接口
        GetByUserReq req = new GetByUserReq(userId);
        GetByUserResp resp = (GetByUserResp) req.executeTillNoNextPage(corpId);
        if (resp == null){
            log.info("[批量获取客户详情] 该员工没有添加客户, corpId:{}, userId:{}", corpId, userId);
            return;
        }
        Map<String, String> userIdInDbMap = weUserService.list(new LambdaQueryWrapper<WeUser>().select(WeUser::getUserId).eq(WeUser::getCorpId, corpId))
                .stream().collect(Collectors.toMap(WeUser::getUserId, WeUser::getUserId));
        // 2. 数据处理:对返回的数据进行处理
        resp.handleData(corpId, userIdInDbMap);
        if (resp.isEmptyResult()) {
            log.info("[批量获取客户详情] 该员工没有添加客户, corpId:{}, userId:{}", corpId, userId);
            return;
        }
        // 3. 数据对齐:本地成员-客户关系数据与服务端对齐,同步远端修改的数据
        weFlowerCustomerRelService.alignData(resp, userId, corpId);
        List<String> externalUserIdList = resp.getCustomerList().stream().map(WeCustomer::getExternalUserid).collect(Collectors.toList());
        List<WeFlowerCustomerRel> localRelList = weFlowerCustomerRelService.list(
                new LambdaQueryWrapper<WeFlowerCustomerRel>()
                        .eq(WeFlowerCustomerRel::getCorpId, corpId)
                        .eq(WeFlowerCustomerRel::getUserId, userId)
                        .in(WeFlowerCustomerRel::getExternalUserid, externalUserIdList)
        );
        if (CollectionUtils.isNotEmpty(localRelList)) {
            updateCustomerRel(resp, localRelList, corpId);
        }
        // 对以前删除但是重新加回的客户重新把状态设置为正常
        resp.activateDelCustomer(localRelList);
        //**** 客户信息更新、插入 , 客户-成员关系更新 , 客户-标签关系更新
        // 4. 数据同步:插入、更新 客户数据,员工-客户关系
        BatchInsertUtil.doInsert(resp.getCustomerList(), this::batchInsert);
        BatchInsertUtil.doInsert(resp.getRelList(), list -> weFlowerCustomerRelService.batchInsert(list));

        // 5. 数据同步: 客户-标签关系 ,获取每个客户关系对应的标签,并同步更新数据库
        List<WeFlowerCustomerTagRel> tagRelList = resp.getCustomerTagRelList(localRelList);
        BatchInsertUtil.doInsert(tagRelList, list -> weFlowerCustomerTagRelService.batchInsert(list));
    }

    /**
     * 更新流失后添加的客户数据，客户-员工关系
     *
     * @param resp 企微响应
     * @param localRelList  本地数据
     */
    public void updateCustomerRel(GetByUserResp resp, List<WeFlowerCustomerRel> localRelList, String corpId){
        List<WeFlowerCustomerRel> updateRelList = new ArrayList<>();
        Map<String, WeFlowerCustomerRel> localMap = localRelList.stream().collect(Collectors.toMap(WeFlowerCustomerRel::getExternalUserid, Function.identity()));
        // 本地数据与远端数据对比
        for (WeFlowerCustomerRel remoteFlowerCustomerRel : resp.getRelList()) {
            WeFlowerCustomerRel localFlowerCustomerRel = localMap.get(remoteFlowerCustomerRel.getExternalUserid());
            // 若该客户与本地记录的添加时间不一样，表示此客户是流失后重新添加员工的客户，将该客户存入列表等待更新状态
            if(localFlowerCustomerRel != null){
                if (remoteFlowerCustomerRel.getCreateTime().compareTo(localFlowerCustomerRel.getCreateTime()) != 0) {
                    remoteFlowerCustomerRel.setStatus(Constants.NORMAL_CODE);
                    updateRelList.add(remoteFlowerCustomerRel);
                }
            }
        }
        // 存在创建时间不同的客户则更新客户状态
        if (CollectionUtils.isNotEmpty(updateRelList)) {
            LambdaUpdateWrapper<WeFlowerCustomerRel> updateRelWrapper = new LambdaUpdateWrapper<>();
            updateRelWrapper.eq(WeFlowerCustomerRel::getCorpId, corpId);
            weFlowerCustomerRelService.batchUpdateStatus(updateRelList);
        }
    }

    /**
     * 调用企微根据corpId获取离职员工列表
     *
     * @param corpId 企业id
     * @return 离职员工列表
     */
    @Override
    public List<LeaveWeUserListsDTO.LeaveWeUser> getLeaveWeUsers(String corpId) {
        LeaveWeUserListsDTO leaveWeUserListsDTO = new LeaveWeUserListsDTO();
        List<LeaveWeUserListsDTO.LeaveWeUser> result = new ArrayList<>();
        do {
            Map<String, Object> map = new HashMap<>();
            //判断是不是第一次获取，不是的话
            if (StringUtils.isNotBlank(leaveWeUserListsDTO.getNext_cursor())) {
                map.put("cursor", leaveWeUserListsDTO.getNext_cursor());
            }
            //企微获取数据
            leaveWeUserListsDTO = weUserClient.leaveWeUsers(map, corpId);

            if (CollUtil.isEmpty(leaveWeUserListsDTO.getInfo())) {
                continue;
            }
            //判断是不是第一次获取
            result.addAll(leaveWeUserListsDTO.getInfo());
        } while (StringUtils.isNotBlank(leaveWeUserListsDTO.getNext_cursor()));

        return result;
    }

    /**
     * @param leaveWeUsers 离职员工列表
     * @return map（离职员工id+","+"离职时间"，客户集合）
     */
    @Override
    public Map<String, List<String>> replaceCustomerListToMap(List<LeaveWeUserListsDTO.LeaveWeUser> leaveWeUsers) {

        if (CollUtil.isEmpty(leaveWeUsers)) {
            return new HashMap<>();
        }
        Map<String, List<String>> map = new HashMap<>(leaveWeUsers.size());

        for (LeaveWeUserListsDTO.LeaveWeUser leaveWeUser : leaveWeUsers) {
            if (map.containsKey(leaveWeUser.getHandover_userid() + "," + leaveWeUser.getDimission_time())) {
                //如果map已存在数据，就修改集合
                List<String> listExternalUserid = map.get(leaveWeUser.getHandover_userid() + "," + leaveWeUser.getDimission_time());
                listExternalUserid.add(leaveWeUser.getExternal_userid());
                map.put(leaveWeUser.getHandover_userid() + "," + leaveWeUser.getDimission_time(), listExternalUserid);
            } else {
                //客户的离职客户id列表
                List<String> listExternalUserid = new ArrayList<>(leaveWeUsers.size());
                listExternalUserid.add(leaveWeUser.getExternal_userid());
                //userId,时间，离职时间
                map.put(leaveWeUser.getHandover_userid() + "," + leaveWeUser.getDimission_time(), listExternalUserid);
            }
        }
        return map;
    }

    /**
     * 客户批量打标签
     *
     * @param list
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchMakeLabel(List<WeMakeCustomerTagVO> list, String updateBy) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        for (WeMakeCustomerTagVO item : list) {
            IncrementalMarkTag(item);
            // 有操作人才需要记录信息动态 且 修改了标签
            if (StringUtils.isNotBlank(updateBy) && !(CollUtil.isEmpty(item.getAddTag()) && CollUtil.isEmpty(item.getRemoveTags()))) {
                weCustomerTrajectoryService.recordEditTagOperation(item.getCorpId(), item.getUserId(), item.getExternalUserid(), updateBy);
            }

        }

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void singleMarkLabel(String corpId, String userId, String externalUserId, List<String> addTagIds, String oprUserName){
        if (StringUtils.isAnyBlank(corpId, userId, externalUserId) || CollUtil.isEmpty(addTagIds)) {
            log.info("员工id，客户id，公司id不能为空，userId：{}，externalUserId：{}，corpId：{}", userId, externalUserId, corpId);
            return;
        }
        WeCustomerService weCustomerService = (WeCustomerService)AopContext.currentProxy();
        weCustomerService.batchMarkCustomTagWithTagIds(corpId, userId, externalUserId, addTagIds, null);
        // 有操作人才需要记录信息动态 且 修改了标签
        if (StringUtils.isNotBlank(oprUserName) && CollUtil.isNotEmpty(addTagIds)) {
            weCustomerTrajectoryService.recordEditTagOperation(corpId, userId, externalUserId, oprUserName);
        }
    }

    /**
     * 增量式打标签
     *
     * @param weMakeCustomerTag
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public void IncrementalMarkTag(WeMakeCustomerTagVO weMakeCustomerTag) {
        if (StringUtils.isAnyBlank(weMakeCustomerTag.getUserId(), weMakeCustomerTag.getExternalUserid(), weMakeCustomerTag.getCorpId())) {
            log.error("员工id，客户id，公司id不能为空，userId：{}，externalUserId：{}，corpId：{}", weMakeCustomerTag.getUserId(), weMakeCustomerTag.getExternalUserid(), weMakeCustomerTag.getCorpId());
            throw new CustomException("增量式打标签错误");
        }
        //增量标签
        List<WeTag> addTags = weMakeCustomerTag.getAddTag();
        // 移除标签
        List<WeTag> removeTags = weMakeCustomerTag.getRemoveTags();
        if (CollUtil.isEmpty(addTags) && CollUtil.isEmpty(removeTags)) {
            return;
        }
        List<String> addTagIds = addTags == null ? Collections.emptyList() : addTags.stream().map(WeTag::getTagId).filter(Objects::nonNull).collect(Collectors.toList());
        List<String> removeTagIds = removeTags == null ? Collections.emptyList() : removeTags.stream().map(WeTag::getTagId).filter(Objects::nonNull).collect(Collectors.toList());
        WeCustomerService weCustomerService = (WeCustomerService)AopContext.currentProxy();
        weCustomerService.batchMarkCustomTagWithTagIds(weMakeCustomerTag.getCorpId(), weMakeCustomerTag.getUserId(), weMakeCustomerTag.getExternalUserid(), addTagIds, removeTagIds);
    }

    /**
     * 删除所有标签关系
     *
     * @param flowerCustomerRel 关系实体
     */
    @Transactional
    public void removeAllLabel(WeFlowerCustomerRel flowerCustomerRel) {
        if (StringUtils.isBlank(flowerCustomerRel.getCorpId())) {
            log.error("员工id，客户id，公司id不能为空，corpId：{}", flowerCustomerRel.getCorpId());
            throw new CustomException("删除所有标签关系");
        }
        //构造查询条件
        LambdaQueryWrapper<WeFlowerCustomerTagRel> queryWrapper = new LambdaQueryWrapper<WeFlowerCustomerTagRel>()
                .eq(WeFlowerCustomerTagRel::getFlowerCustomerRelId, flowerCustomerRel.getId());
        List<WeFlowerCustomerTagRel> removeTag = weFlowerCustomerTagRelService.list(queryWrapper);

        //官方接口删除
        if (!CollectionUtils.isEmpty(removeTag) && weFlowerCustomerTagRelService.remove(queryWrapper)) {
            //企微新接口
            weCustomerClient.makeCustomerLabel(
                    CustomerTagEdit.builder()
                            .external_userid(flowerCustomerRel.getExternalUserid())
                            .userid(flowerCustomerRel.getUserId())
                            .remove_tag(ArrayUtil.toArray(removeTag.stream().map(WeFlowerCustomerTagRel::getTagId).collect(Collectors.toList()), String.class))
                            .build(), flowerCustomerRel.getCorpId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeLabel(RemoveWeCustomerTagDTO removeWeCustomerTagDTO) {
        List<String> removeTagList = removeWeCustomerTagDTO.getWeTagIdList();
        List<RemoveWeCustomerTagDTO.WeUserCustomer> userCustomerList = removeWeCustomerTagDTO.getCustomerList();
        if (CollUtil.isEmpty(removeTagList) || CollUtil.isEmpty(userCustomerList)) {
            throw new WeComException("请传入需要删除的标签和对应的用户");
        }

        // 调用企微API依次删除客户标签，删除成功后操作数据库(目前企微没有批量编辑标签接口,只能单个删除)
        for (RemoveWeCustomerTagDTO.WeUserCustomer userCustomer : userCustomerList) {
            if (StringUtils.isBlank(userCustomer.getCorpId())) {
                log.error("删除标签失败,公司id不能为空", userCustomer.getCorpId());
                throw new CustomException("删除标签失败");
            }

            CustomerTagEdit customerTagEdit = CustomerTagEdit.builder()
                    .external_userid(userCustomer.getExternalUserid())
                    .userid(userCustomer.getUserId())
                    .remove_tag(ArrayUtil.toArray(removeTagList, String.class))
                    .build();
            //企微新接口
            try {
                WeResultDTO response = weCustomerClient.makeCustomerLabel(customerTagEdit, userCustomer.getCorpId());
                if (response != null && response.isSuccess()) {
                    weFlowerCustomerTagRelService.removeByCustomerIdAndUserId(userCustomer.getExternalUserid(), userCustomer.getUserId(), userCustomer.getCorpId(), removeTagList);
                }
            } catch (ForestRuntimeException e) {
                log.error("获取客户数据失败 e:{}", ExceptionUtils.getStackTrace(e));
            }
        }

    }


    @Override
    public WeCustomerVO getCustomerByUserId(String externalUserid, String userId, String corpId) {
        List<WeCustomerVO> list = getCustomersByUserIdV2(externalUserid, userId, corpId);
        if (CollectionUtils.isEmpty(list)) {
            throw new CustomException(ResultTip.TIP_CUSTOMER_NOT_EXIST);
        }
        return list.get(0);
    }


    @Override
    public List<WeCustomerVO> getCustomersByUserIdV2(String externalUserid, String userId, String corpId) {
        if (StringUtils.isAnyBlank(externalUserid, userId, corpId)) {
            throw new CustomException(ResultTip.TIP_GENERAL_PARAM_ERROR);
        }
        return weCustomerMapper.getCustomersByUserIdV2(externalUserid, userId, corpId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateExternalContactV2(String corpId, String userId, String externalUserid) {
        if (StringUtils.isAnyBlank(corpId, userId, externalUserid)) {
            log.info("更新客户,参数缺失,corpId:{},userId:{},externalUserid:{}", corpId, userId, externalUserid);
            return;
        }
        // 1. 接口调用: 调用企微API 获取外部联系人详情
        GetExternalDetailResp resp = weCustomerClient.getV2(externalUserid, corpId);
        if (resp.isEmptyResult()) {
            return;
        }
        // 2. 数据处理: 根据返回数据构建与数据库交互的实体
        resp.handleData(corpId, userId);
        if (resp.getRemoteRel() != null) {
            weFlowerCustomerRelService.insert(resp.getRemoteRel());
        }
        WeFlowerCustomerRel localRel = weFlowerCustomerRelService.getOne(new LambdaQueryWrapper<WeFlowerCustomerRel>()
                .eq(WeFlowerCustomerRel::getCorpId, corpId)
                .eq(WeFlowerCustomerRel::getExternalUserid, externalUserid)
                .eq(WeFlowerCustomerRel::getUserId, userId)
                .last(GenConstants.LIMIT_1)
        );

        // 根据本地客户跟进人关系和返回标签组构建 客户标签关系实体
        List<WeFlowerCustomerTagRel> tagRelList = resp.getTagRelList(localRel);

        // 3. 数据更新：插入/更新数据库里的客户信息,员工客户关系
        this.insert(resp.getRemoteCustomer());

        // 4. 数据更新：清除所有旧的标签关系,插入当前标签关系
        weFlowerCustomerTagRelService.remove(new LambdaQueryWrapper<WeFlowerCustomerTagRel>()
                .eq(WeFlowerCustomerTagRel::getFlowerCustomerRelId, localRel.getId())
        );
        if (CollectionUtils.isNotEmpty(tagRelList)) {
            BatchInsertUtil.doInsert(tagRelList, list -> weFlowerCustomerTagRelService.batchInsert(list));
        }
    }

    /**
     * 调用企业微信接口发送好友欢迎语
     *
     * @param weWelcomeMsg 消息体
     * @param corpId       企业id
     */
    @Override
    public void sendWelcomeMsg(WeWelcomeMsg weWelcomeMsg, String corpId) {
        weCustomerClient.sendWelcomeMsg(weWelcomeMsg, corpId);
    }


    @Convert2Cipher
    @Override
    public WeCustomerPortrait findCustomerByOperUseridAndCustomerId(String externalUserid, String userid, String corpId) {
        if (StringUtils.isAnyBlank(externalUserid, userid, corpId)) {
            throw new CustomException(ResultTip.TIP_GENERAL_PARAM_ERROR);
        }
        // 查询客户详情
        WeCustomerVO customer = getCustomerByUserId(externalUserid, userid, corpId);
        if (customer == null) {
            throw new CustomException(ResultTip.TIP_CUSTOMER_NOT_EXIST);
        }
        // 转换成客户画像实体
        WeCustomerPortrait weCustomerPortrait = new WeCustomerPortrait(customer);
        // 设置年龄
        if (weCustomerPortrait.getBirthday() != null) {
            weCustomerPortrait.setAge(DateUtils.getAge(weCustomerPortrait.getBirthday()));
        }
        //获取当前客户拥有得标签
        weCustomerPortrait.setWeTagGroupList(
                weTagGroupService.findCustomerTagByFlowerCustomerRelId(weCustomerPortrait.getFlowerCustomerRelId())
        );
        //客户社交关系
        weCustomerPortrait.setSocialConn(
                this.baseMapper.countSocialConn(externalUserid, userid, corpId)
        );
        return weCustomerPortrait;
    }

    @Override
    @DataScope
    public List<WeCustomer> selectWeCustomerListNoRel(WeCustomerPushMessageDTO weCustomer) {
        if (StringUtils.isBlank(weCustomer.getCorpId())) {
            log.error("获取客户列表失败，corpId不能为空");
            throw new CustomException("获取客户列表失败");
        }
        WeCustomerPushMessageDTO buildWeCustomer = weCustomer;
        //查找部门下员工
        if (StringUtils.isNotEmpty(weCustomer.getDepartmentIds())) {
            List<String> userIdsByDepartment = weUserService.listOfUserId(weCustomer.getCorpId(), weCustomer.getDepartmentIds().split(StrUtil.COMMA));
            String userIdsFromDepartment = CollectionUtils.isNotEmpty(userIdsByDepartment) ? StringUtils.join(userIdsByDepartment, WeConstans.COMMA) : StringUtils.EMPTY;
            if (StringUtils.isNotEmpty(buildWeCustomer.getUserIds())) {
                buildWeCustomer.setUserIds(buildWeCustomer.getUserIds() + StrUtil.COMMA + userIdsFromDepartment);
            } else {
                buildWeCustomer.setUserIds(userIdsFromDepartment);
            }
        }
        if (WeConstans.SEND_MESSAGE_CUSTOMER_PART.equals(weCustomer.getPushRange()) && StringUtils.isBlank(buildWeCustomer.getUserIds())) {
            return Collections.emptyList();
        }
        return weCustomerMapper.selectWeCustomerListNoRel(buildWeCustomer);
    }

    @Override
    public List<CustomerSopVO> listOfCustomerIdAndUserId(String corpId, String userIds, @NotBlank List<String> customerIds) {
        if (StringUtils.isBlank(corpId)) {
            throw new CustomException(ResultTip.TIP_MISS_CORP_ID);
        }
        return weCustomerMapper.listOfCustomerIdAndUserId(corpId, userIds, customerIds);
    }

    @Override
    public Integer customerCount(String corpId) {
        if (org.apache.commons.lang3.StringUtils.isBlank(corpId)) {
            throw new CustomException(ResultTip.TIP_GENERAL_BAD_REQUEST);
        }
        return this.getBaseMapper().countCustomerNum(corpId);
    }

    @Override
    public QueryCustomerFromPlusVO getDetailByUserIdAndCustomerAvatar(String corpId, String userId, String avatar) {
        if (org.apache.commons.lang3.StringUtils.isAnyBlank(corpId, userId, avatar)) {
            return null;
        }
        WeCustomer customer = getOne(new LambdaQueryWrapper<WeCustomer>()
                .eq(WeCustomer::getCorpId, corpId)
                .eq(WeCustomer::getAvatar, avatar)
                .last(GenConstants.LIMIT_1)
        );
        if (customer == null) {
            throw new CustomException(ResultTip.TIP_CUSTOMER_NOT_EXIST);
        }
        QueryCustomerFromPlusVO vo = new QueryCustomerFromPlusVO();
        BeanUtils.copyPropertiesASM(customer, vo);
        // 查询员工-客户关联信息
        WeFlowerCustomerRel rel = weFlowerCustomerRelService.getOne(
                new LambdaQueryWrapper<WeFlowerCustomerRel>()
                        .eq(WeFlowerCustomerRel::getExternalUserid, customer.getExternalUserid())
                        .eq(WeFlowerCustomerRel::getCorpId, corpId)
                        .eq(WeFlowerCustomerRel::getUserId, userId)
                        .last(GenConstants.LIMIT_1)
        );
        if (rel != null) {
            QueryCustomerFromPlusVO.FollowUserInfo followUserInfo = new QueryCustomerFromPlusVO().new FollowUserInfo();
            BeanUtils.copyPropertiesASM(rel, followUserInfo);
            vo.setFollowUserInfo(followUserInfo);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editByUserIdAndCustomerAvatar(EditCustomerFromPlusDTO dto) {
        if (org.apache.commons.lang3.StringUtils.isAnyBlank(dto.getCorpId(), dto.getAvatar(), dto.getUserId())) {
            return;
        }
        // 查询客户是否存在,并获取外部联系人id
        WeCustomer customer = getOne(new LambdaQueryWrapper<WeCustomer>()
                .eq(WeCustomer::getCorpId, dto.getCorpId())
                .eq(WeCustomer::getAvatar, dto.getAvatar())
                .last(GenConstants.LIMIT_1)
        );
        if (customer == null) {
            throw new CustomException(ResultTip.TIP_CUSTOMER_NOT_EXIST);
        }
        // 参数格式转换
        WeCustomer model = new WeCustomer();
        BeanUtils.copyPropertiesASM(dto, model);
        model.setExternalUserid(customer.getExternalUserid());
        // 调用内部修改客户信息接口
        updateWeCustomerRemark(model);
    }

    @Override
    public void batchInsert(List<WeCustomer> customerList) {
        weCustomerMapper.batchInsert(customerList);
    }

    @Override
    public void insert(WeCustomer weCustomer) {
        List<WeCustomer> list = new ArrayList<>();
        list.add(weCustomer);
        this.batchInsert(list);
    }

    @Override
    public <T> AjaxResult<T> export(WeCustomerExportDTO dto) {
        List<String> selectProperties=dto.getSelectedProperties();
        WeCustomerSearchDTO weCustomerSearchDTO=new WeCustomerSearchDTO();
        BeanUtils.copyProperties(dto, weCustomerSearchDTO);
        WeCustomer weCustomer=changeWecustomer(weCustomerSearchDTO);
        List<WeCustomerVO> list = this.selectWeCustomerListV2(weCustomer);
        if (CollectionUtils.isEmpty(list)) {
            throw new CustomException(ResultTip.TIP_NO_DATA_TO_EXPORT);
        }
        List<WeCustomerExportVO> exportList = list.stream().map(WeCustomerExportVO::new).collect(Collectors.toList());
        weCustomerExtendPropertyService.setKeyValueMapper(weCustomer.getCorpId(), exportList, selectProperties);
        ExcelUtil<WeCustomerExportVO> util = new ExcelUtil<>(WeCustomerExportVO.class);
        return util.exportExcelV2(exportList, "customer", selectProperties);
    }

    /**
     * 模糊查询客户 (无需登录可用)
     *
     * @param corpId
     * @param customerName
     * @return
     */
    @Override
    public List<WeCustomerVO> getCustomer(String corpId, String customerName) {
        if (StringUtils.isBlank(corpId)) {
            throw new CustomException(ResultTip.TIP_MISS_CORP_ID);
        }
        return this.baseMapper.listCustomers(customerName, corpId);
    }

    @Override
    public GetUnionIdVO getDetailByExternalUserId(GetUnionIdDTO getUnionIdDTO) {
        if(getUnionIdDTO == null || StringUtils.isAnyBlank(getUnionIdDTO.getCorpId(),getUnionIdDTO.getExternalUserId(), getUnionIdDTO.getCorpSecret())) {
            throw new CustomException(ResultTip.TIP_PARAM_NAME_MISSING);
        }
        String corpSecret = corpSecretDecryptUtil.decryptUnionId(getUnionIdDTO.getCorpSecret()) ;
        ExternalUserDetail userDetail = weUnionIdClient.getByExternalUserId(getUnionIdDTO.getExternalUserId(), getUnionIdDTO.getCorpId(), corpSecret);
        if(userDetail == null || userDetail.getExternal_contact() == null ) {
            throw new CustomException(ResultTip.TIP_GET_UNION_ID_FAIL);
        }
        return new GetUnionIdVO(userDetail.getExternal_contact());
    }

    /**
     * 根据openId获取客户详情
     *
     * @param openId 公众号openid
     * @param corpId
     * @return 客户详情 {@link WeCustomer}
     */
    @Override
    public WeCustomer getCustomerInfoByOpenId(String openId, String corpId) {
        if (StringUtils.isBlank(openId)) {
            return null;
        }
        // 1. 根据openid获取unionId
        String unionId = wechatOpenService.getUnionIdByOpenId(openId);
        //  2. 先根据union_id去客户表查询是否有数据
        WeCustomer customer = getCustomerByUnionId(unionId, openId, corpId);
        if (customer == null) {
            log.info("[根据openId获取客户详情] 获取客户详情,根据union_id在数据库中未匹配到客户,openId:{},unionId:{}", openId, unionId);
            throw new CustomException(ResultTip.TIP_CANNOT_FIND_USER_BY_UNION_ID);
        }
        return customer;
    }

    @Override
    public WeCustomer getCustomerByUnionId(String unionId, String openId, String corpId) {
        if (StringUtils.isAnyBlank(unionId, openId, corpId)) {
            return null;
        }
        WeCustomer customer = this.getOne(new LambdaQueryWrapper<WeCustomer>()
                .eq(WeCustomer::getUnionid, unionId)
                .eq(WeCustomer::getCorpId, corpId)
                .last(GenConstants.LIMIT_1));
        // 如果在数据库中没查到客户信息, 且当前环境为待开发 则调用接口获取externalUserId
        if (customer == null && ServerTypeEnum.THIRD.getType().equals(we3rdAppService.getServerType().getServerType())) {
            ExternalUserDetail externalUserDetail = weCustomerClient.getExternalUserIdByUnionIdAndOpenId(unionId, openId, SubjectTypeEnum.ENTERPRISE.getCode(), corpId);
            if (externalUserDetail == null || StringUtils.isBlank(externalUserDetail.getExternal_userid())) {
                return null;
            }
            // 若正常返回exteranlUserId再查找客户信息，并将unionId写入到到数据库中
            customer = this.getOne(new LambdaQueryWrapper<WeCustomer>()
                    .eq(WeCustomer::getCorpId, corpId)
                    .eq(WeCustomer::getExternalUserid, externalUserDetail.getExternal_userid())
                    .last(GenConstants.LIMIT_1));
            if (customer == null) {
                return null;
            }
            customer.setUnionid(unionId);
            this.update(new LambdaUpdateWrapper<WeCustomer>()
                    .eq(WeCustomer::getExternalUserid, externalUserDetail.getExternal_userid())
                    .eq(WeCustomer::getCorpId, corpId)
                    .set(WeCustomer::getUnionid, unionId));
        }
        return customer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String getOpenExUserId(String corpId, String externalUserId) {
        if (StringUtils.isAnyBlank(corpId, externalUserId)) {
            return StringUtils.EMPTY;
        }
        WeExternalUseridMapping weExternalUseridMapping = weExternalUseridMappingMapper.selectOne(new LambdaUpdateWrapper<WeExternalUseridMapping>()
                .eq(WeExternalUseridMapping::getCorpId, corpId)
                .eq(WeExternalUseridMapping::getExternalUserid, externalUserId)
                .last(GenConstants.LIMIT_1));
        if (weExternalUseridMapping == null) {
            String openExUserId = getOpenExUserIdByClient(corpId, externalUserId);
            weExternalUseridMapping = new WeExternalUseridMapping(corpId, externalUserId, openExUserId);
            weExternalUseridMappingMapper.insertOrUpdate(weExternalUseridMapping);
        }
        return weExternalUseridMapping.getOpenExternalUserid();
    }

    /**
     * 通过接口将外部联系人exUserId转化为密文
     *
     * @param corpId            企业id
     * @param externalUserId    外部联系人exUserId
     * @return
     */
    protected String getOpenExUserIdByClient(String corpId, String externalUserId) {
        List<String> externalUserIds = new ArrayList<>();
        externalUserIds.add(externalUserId);
        final CorpIdToOpenCorpIdResp newExternalUser = convertIDClient.getNewExternalUserid(corpId, externalUserIds);
        Map<String,String> openExternalUserIdMap = newExternalUser.getItems().stream().collect(Collectors.toMap(CorpIdToOpenCorpIdResp.ExternalUserMapping::getExternal_userid, CorpIdToOpenCorpIdResp.ExternalUserMapping::getNew_external_userid));
        return openExternalUserIdMap.getOrDefault(externalUserId, com.easyink.common.utils.StringUtils.EMPTY);
    }

}
