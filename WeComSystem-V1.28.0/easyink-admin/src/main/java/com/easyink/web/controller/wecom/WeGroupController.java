package com.easyink.web.controller.wecom;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.easyink.common.constant.WeConstans;
import com.easyink.common.core.controller.BaseController;
import com.easyink.common.core.domain.AjaxResult;
import com.easyink.common.core.page.TableDataInfo;
import com.easyink.wecom.domain.vo.AllocateWeGroupResp;
import com.easyink.wecom.domain.WeGroup;
import com.easyink.wecom.domain.WeGroupMember;
import com.easyink.wecom.domain.dto.FindWeGroupDTO;
import com.easyink.wecom.domain.dto.FindWeGroupMemberDTO;
import com.easyink.wecom.domain.vo.FindWeGroupMemberCountVO;
import com.easyink.wecom.domain.vo.WeLeaveAllocateVO;
import com.easyink.wecom.login.util.LoginTokenService;
import com.easyink.wecom.service.WeGroupMemberService;
import com.easyink.wecom.service.WeGroupService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 群组相关
 *
 * @author admin
 * @Description:
 * @Date: create in 2020/9/21 0021 23:53
 */

@RestController
@RequestMapping("/wecom/group/chat")
@Slf4j
@Api(tags = "群组相关")
public class WeGroupController extends BaseController {
    @Autowired
    private WeGroupService weGroupService;

    @Autowired
    private WeGroupMemberService weGroupMemberService;

    @GetMapping({"/list"})
    @ApiOperation("查看群列表")
    public TableDataInfo<WeGroup> list(FindWeGroupDTO weGroup) {
        startPage();
        weGroup.setCorpId(LoginTokenService.getLoginUser().getCorpId());
        List<WeGroup> list = weGroupService.list(weGroup);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('customerManage:group:export')")
    @GetMapping("/export")
    @ApiOperation("导出客户群")
    public <T> AjaxResult export(FindWeGroupDTO weGroup) {
        weGroup.setCorpId(LoginTokenService.getLoginUser().getCorpId());
        return weGroupService.export(weGroup);
    }

    @PreAuthorize("@ss.hasPermi('customerManage:group:view')")
    @GetMapping({"/members"})
    @ApiOperation("群成员列表")
    public TableDataInfo<WeGroupMember> list(FindWeGroupMemberDTO weGroupMember) {
        startPage();
        weGroupMember.setCorpId(LoginTokenService.getLoginUser().getCorpId());
        List<WeGroupMember> list = this.weGroupMemberService.selectWeGroupMemberList(weGroupMember);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('customerManage:group:sync')")
    @GetMapping({"/synchWeGroup"})
    @ApiOperation("同步客户群")
    public AjaxResult<T> synchWeGroup() {
        try {
            SecurityContext context = SecurityContextHolder.getContext();
            SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
            SecurityContextHolder.setContext(context);
            weGroupService.syncWeGroup(LoginTokenService.getLoginUser().getCorpId());
        } catch (Exception e) {
            log.error("同步客户群异常 ex:{}", ExceptionUtils.getStackTrace(e));
        }
        return AjaxResult.success(WeConstans.SYNCH_TIP);
    }

    /**
     * 根据员工id获取员工相关群
     *
     * @param userId
     * @return
     */
    @GetMapping({"/getGroupsByUserId/{userId}"})
    @ApiOperation("根据员工id获取员工相关群")
    public AjaxResult<T> getGroupsByUserId(@PathVariable String userId) {
        return AjaxResult.success(weGroupService
                .list(new LambdaQueryWrapper<WeGroup>().eq(WeGroup::getOwner, userId)));
    }



    @GetMapping(value = "/allocatedStaffDetail")
    @ApiOperation("根据群聊id获取群聊详情")
    public AjaxResult<T> allocatedStaffDetail(@NotBlank(message = "群id不能为空") String charId){
        return AjaxResult.success(weGroupService.selectWeGroupDetail(charId,LoginTokenService.getLoginUser().getCorpId()));
    }


    @GetMapping(value = "/getMemberCount")
    @ApiOperation("获取群成员数量")
    public AjaxResult<FindWeGroupMemberCountVO> getMemberCount(FindWeGroupMemberDTO findWeGroupMemberDTO){
        findWeGroupMemberDTO.setCorpId(LoginTokenService.getLoginUser().getCorpId());
        return AjaxResult.success(weGroupMemberService.selectWeGroupMemberCount(findWeGroupMemberDTO));
    }

}
