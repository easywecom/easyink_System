package com.easyink.wecom.domain.vo;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import com.alibaba.excel.annotation.write.style.HeadStyle;
import com.baomidou.mybatisplus.annotation.TableField;
import com.easyink.common.annotation.Excel;
import com.easyink.common.utils.StringUtils;
import com.easyink.common.utils.bean.BeanUtils;
import com.easyink.wecom.domain.WeCustomer;
import com.easyink.wecom.domain.WeFlowerCustomerTagRel;
import com.easyink.wecom.domain.vo.customer.WeCustomerVO;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 类名: 导出客户详情EXCEL VO
 *
 * @author : silver_chariot
 * @date : 2021/9/26 13:49
 */
@Data
@ApiModel(value = "导出客户详情数据实体")
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@ContentRowHeight(10)
@HeadRowHeight(20)
@ColumnWidth(35)
public class WeCustomerExportVO extends WeCustomer {


    @ApiModelProperty(value = "添加方式,0=未知来源,1=扫描二维码,2=搜索手机号,3=名片分享,4=群聊,5=手机通讯录,6=微信联系人,7=来自微信的添加好友申请,8=安装第三方应用时自动添加的客服人员,9=搜索邮箱,16=通过获客链接添加,201=内部成员共享,202=管理员负责人分配")
    @TableField(exist = false)
    @Excel(name = "来源",  readConverterExp = "0=未知来源,1=扫描二维码,2=搜索手机号,3=名片分享,4=群聊,5=手机通讯录,6=微信联系人,7=来自微信的添加好友申请,8=安装第三方应用时自动添加的客服人员,9=搜索邮箱,16=通过获客链接添加,201=内部成员共享,202=管理员负责人分配")
    @ExcelProperty(value = "来源", index = 13)
    private String addWay;

    @ApiModelProperty(value = "添加时间")
    @TableField(exist = false)
    @Excel(name = "添加时间",  dateFormat = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ExcelProperty(value = "添加时间", index = 4)
    @DateTimeFormat("yyyy-MM-dd")
    private Date createTime;

    @ApiModelProperty(value = "该成员对此外部联系人的描述")
    @TableField(exist = false)
    @Excel(name = "描述" )
    @ExcelProperty(value = "描述", index = 14)
    private String description;


    @ApiModelProperty(value = "标签")
    @TableField(exist = false)
    @Excel(name = "标签")
    @ExcelProperty(value = "标签", index = 7)
    private String tags;

    @TableField(exist = false)
    @Excel(name = "客户状态"  ,readConverterExp = "0=正常,1=已流失,2=已流失,3=待继承,4=转接中" , defaultValue = "待继承")
    @ExcelProperty(value = "客户状态", index = 8)
    private String customerStatus;

    @TableField(exist = false)
    @Excel(name = "邮箱")
    @ExcelProperty(value = "邮箱", index = 11)
    private String email;

    @Excel(name = "地址")
    @ExcelProperty(value = "地址", index = 12)
    private String address;

    @TableField(exist = false)
    @Excel(name = "unionId")
    @ExcelProperty(value = "unionId", index = 15)
    private String unionId;

    @TableField(exist = false)
    @Excel(name = "externalUserId")
    @ExcelProperty(value = "externalUserId", index = 16)
    private String externalUserId;

    @TableField(exist = false)
    @Excel(name = "externalUserId（明文）")
    @ExcelProperty(value = "externalUserId（明文）", index = 16)
    private String originExternalUserId;
    /**
     * 扩展属性与值的映射,K:扩展属性名字,V该客户对应的值
     */
    @ExcelProperty
    private Map<String, String> extendPropMapper;

    public WeCustomerExportVO(WeCustomerVO weCustomer) {
        BeanUtils.copyProperties(weCustomer, this);
        // 标签格式转换成字符串
        this.customerStatus = weCustomer.getStatus();
        this.unionId = weCustomer.getUnionId();
        this.setPhone(weCustomer.getRemarkMobiles());
        this.externalUserId = weCustomer.getExternalUserid();
        this.originExternalUserId = weCustomer.getOriginExternalUserId();
        if (CollectionUtils.isEmpty(this.getExtendProperties())){
            this.setExtendProperties(new ArrayList<>());
        }
        if (CollUtil.isNotEmpty(weCustomer.getWeFlowerCustomerTagRels())) {
            try {
                List<WeFlowerCustomerTagRel> weFlowerCustomerTagRelList = weCustomer.getWeFlowerCustomerTagRels();
                this.tags = StringUtils.join(weFlowerCustomerTagRelList.stream().map(WeFlowerCustomerTagRel::getTagName).toArray(), ",");
            } catch (Exception e) {
                log.info("获取用户所有标签：集合转字符串异常,e:{}", ExceptionUtils.getStackTrace(e));
            }
        }
    }


}
