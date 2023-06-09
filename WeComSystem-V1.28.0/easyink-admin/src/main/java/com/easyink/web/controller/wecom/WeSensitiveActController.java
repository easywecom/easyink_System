package com.easyink.web.controller.wecom;

import com.easyink.common.annotation.Log;
import com.easyink.common.core.controller.BaseController;
import com.easyink.common.core.domain.AjaxResult;
import com.easyink.common.core.domain.RootEntity;
import com.easyink.common.core.page.TableDataInfo;
import com.easyink.common.enums.BusinessType;
import com.easyink.common.enums.ResultTip;
import com.easyink.common.utils.poi.ExcelUtil;
import com.easyink.wecom.domain.WeSensitiveAct;
import com.easyink.wecom.domain.WeSensitiveActHit;
import com.easyink.wecom.login.util.LoginTokenService;
import com.easyink.wecom.service.WeSensitiveActHitService;
import com.easyink.wecom.service.WeSensitiveActService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 敏感行为管理接口
 *
 * @author admin
 * @version 1.0
 * @date 2021/1/12 18:07
 */
@RestController
@RequestMapping("/wecom/sensitive/act")
@Api(tags = "敏感行为管理接口")
public class WeSensitiveActController extends BaseController {
    private final WeSensitiveActService weSensitiveActService;
    private final WeSensitiveActHitService weSensitiveActHitService;

    @Autowired
    public WeSensitiveActController(@NotNull WeSensitiveActService weSensitiveActService, @NotNull WeSensitiveActHitService weSensitiveActHitService) {
        this.weSensitiveActHitService = weSensitiveActHitService;
        this.weSensitiveActService = weSensitiveActService;
    }

    @PreAuthorize("@ss.hasPermi('wecom:sensitiveact:list')")
    @GetMapping("/list")
    @ApiOperation("查询敏感行为列表")
    public TableDataInfo list(WeSensitiveAct weSensitiveAct) {
        startPage();
        String corpId = LoginTokenService.getLoginUser().getCorpId();
        if (StringUtils.isNotBlank(corpId)){
            weSensitiveAct.setCorpId(corpId);
        }
        List<WeSensitiveAct> list = weSensitiveActService.selectWeSensitiveActList(weSensitiveAct);
        return getDataTable(list);
    }

    /**
     * 获取敏感行为详细信息
     */
    //   @PreAuthorize("@ss.hasPermi('wecom:sensitiveact:query')")
    @GetMapping(value = "/{id}")
    @ApiOperation("获取敏感行为详细信息")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(weSensitiveActService.selectWeSensitiveActById(id));
    }

    /**
     * 新增敏感行为设置
     * @Deprecated 该接口暂时未暴露给前端调用
     */
    @Deprecated
    @PreAuthorize("@ss.hasPermi('wecom:sensitiveact:add')")
    @Log(title = "新增敏感行为", businessType = BusinessType.INSERT)
    @PostMapping
    @ApiOperation("新增敏感行为设置")
    public AjaxResult add(@Valid @RequestBody WeSensitiveAct weSensitiveAct) {
        return weSensitiveActService.insertWeSensitiveAct(weSensitiveAct) ? AjaxResult.success() : AjaxResult.error();
    }

    /**
     * 修改敏感词设置
     */
    @PreAuthorize("@ss.hasPermi('wecom:sensitiveact:edit')")
    @Log(title = "修改敏感行为", businessType = BusinessType.UPDATE)
    @PutMapping
    @ApiOperation("修改敏感词设置")
    public AjaxResult edit(@RequestBody WeSensitiveAct weSensitiveAct) {
        Long id = weSensitiveAct.getId();
        WeSensitiveAct originData = weSensitiveActService.selectWeSensitiveActById(id);
        if (originData == null) {
            return AjaxResult.error(ResultTip.TIP_GENERAL_NOT_FOUND, "数据不存在");
        }
        return weSensitiveActService.updateWeSensitiveAct(weSensitiveAct) ? AjaxResult.success() : AjaxResult.error();
    }

    /**
     * 删除敏感词设置
     */
    @PreAuthorize("@ss.hasPermi('wecom:sensitiveact:remove')")
    @Log(title = "删除敏感行为", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    @ApiOperation("删除敏感词设置")
    public AjaxResult remove(@PathVariable("ids") String ids) {
        String[] id = ids.split(",");
        Long[] idArray = new Long[id.length];
        Arrays.stream(id).map(Long::parseLong).collect(Collectors.toList()).toArray(idArray);
        return weSensitiveActService.deleteWeSensitiveActByIds(idArray) ? AjaxResult.success() : AjaxResult.error();
    }

    /**
     * 敏感词命中查询
     */
    //    @PreAuthorize("@ss.hasPermi('wecom:sensitiveacthit:list')")
    @GetMapping("/hit/list")
    @ApiOperation("敏感词命中查询")
    public TableDataInfo hitList() {
        startPage();
        RootEntity rootEntity = new RootEntity();
        rootEntity.getParams().put("corpId", LoginTokenService.getLoginUser().getCorpId());
        List<WeSensitiveActHit> list = weSensitiveActHitService.selectWeSensitiveActHitList(rootEntity);
        return getDataTable(list);
    }

    /**
     * 导出敏感行为记录
     * @Deprecated 暂未使用
     */
    @Deprecated
    //   @PreAuthorize("@ss.hasPermi('wecom:sensitiveacthit:export')")
    @PostMapping("/hit/export")
    @ApiOperation("导出敏感行为记录")
    public AjaxResult export() {
        List<WeSensitiveActHit> list = weSensitiveActHitService.list();
        ExcelUtil<WeSensitiveActHit> util = new ExcelUtil<>(WeSensitiveActHit.class);
        return util.exportExcel(list, "敏感行为记录");
    }
}
