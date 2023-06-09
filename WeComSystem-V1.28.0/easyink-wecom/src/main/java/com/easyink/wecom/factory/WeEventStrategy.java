package com.easyink.wecom.factory;

import com.easyink.common.constant.WeConstans;
import com.easyink.common.core.domain.wecom.WeDepartment;
import com.easyink.common.core.domain.wecom.WeUser;
import com.easyink.common.utils.StringUtils;
import com.easyink.wecom.domain.vo.WxCpXmlMessageVO;

import java.util.Arrays;

/**
 * @author admin
 * @description 事件类型策略接口
 * @date 2021/1/20 22:00
 **/
public abstract class WeEventStrategy {
    protected final String tag = "tag";
    protected final String tagGroup = "tag_group";

    public abstract void eventHandle(WxCpXmlMessageVO message);

    //生成成员数据
    public WeUser setWeUserData(WxCpXmlMessageVO message) {
        WeUser weUser = WeUser.builder().userId(message.getUserId())
                .email(message.getEmail())
                .name(message.getName())
                .alias(message.getAlias())
                .gender(message.getGender())
                .address(message.getAddress())
                .telephone(message.getTelephone())
                .mobile(message.getMobile())
                .avatarMediaid(message.getAvatar())
                .position(message.getPosition())
                //设置企业ID
                .corpId(message.getToUserName())
                .build();
        if (message.getStatus() != null) {
            weUser.setIsActivate(Integer.valueOf(message.getStatus()));
        }
        if (message.getIsLeaderInDept() != null) {
            String[] isLeaderInDeptArr = Arrays.stream(message.getIsLeaderInDept())
                    .map(String::valueOf).toArray(String[]::new);
            weUser.setIsLeaderInDept(isLeaderInDeptArr);
        }
        if (message.getDepartments() != null) {
            String[] departmentsArr = Arrays.stream(message.getDepartments())
                    .map(String::valueOf).toArray(String[]::new);
            weUser.setDepartment(departmentsArr);
        }
        if (message.getMainDepartment() != null) {
            weUser.setMainDepartment(Long.valueOf(message.getMainDepartment()));
        } else {
            weUser.setMainDepartment(Long.parseLong(WeConstans.OTHER_USER_DEPARTMENT));
        }
        return weUser;
    }

    //部门信息
    public WeDepartment setWeDepartMent(WxCpXmlMessageVO message) {
        WeDepartment weDepartment = new WeDepartment();
        if (message.getId() != null) {
            weDepartment.setId(message.getId());
        }
        if (message.getName() != null) {
            weDepartment.setName(message.getName());
        }
        if (message.getParentId() != null) {
            weDepartment.setParentId(Long.valueOf(message.getParentId()));
        }
        // 设置公司id
        if (StringUtils.isNotBlank(message.getToUserName())) {
            weDepartment.setCorpId(message.getToUserName());
        }
        return weDepartment;
    }
}
