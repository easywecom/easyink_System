package com.easyink.wecom.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @author admin
 * @description
 * @date 2021/2/25 11:21
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WePageCountDTO {
    /**
     * 日期 , 格式 ：  '%Y-%m-%d'
     */
    private String xTime;

    /**
     * 发起申请数
     */
    private Integer newApplyCnt;

    /**
     * 新增客户数，成员新添加的客户数量
     */
    private Integer newContactCnt;

    /**
     * 聊天总数， 成员有主动发送过消息的单聊总数
     */
    private Integer chatCnt;

    /**
     * 发送消息数，成员在单聊中发送的消息总数
     */
    private Integer messageCnt;

    /**
     * 已回复聊天占比，浮点型，客户主动发起聊天后，成员在一个自然日内有回复过消息的聊天数/客户主动发起的聊天数比例，不包括群聊，仅在确有聊天时返回
     */
    private Float replyPercentage;

    /**
     * 平均首次回复时长
     */
    private Integer avgReplyTime;

    /**
     * 删除/拉黑成员的客户数，即将成员删除或加入黑名单的客户数
     */
    private Integer negativeFeedbackCnt;

    /**
     * 新增客户群数量
     */
    private Integer newChatCnt;

    /**
     * 截至当天客户群总数量
     */
    private Integer chatTotal;

    /**
     * 截至当天有发过消息的客户群数量
     */
    private Integer chatHasMsg;

    /**
     * 客户群新增群人数
     */
    private Integer newMemberCnt;

    /**
     * 截至当天客户群总人数
     */
    private Integer memberTotal;

    /**
     * 截至当天有发过消息的群成员数
     */
    private Integer memberHasMsg;

    /**
     * 截至当天客户群消息总数
     */
    private Integer msgTotal;
    /**
     * 新客留存率
     */
    private String newContactRetentionRate;

    /**
     * 总客户人数
     */
    private Integer totalContactCnt;
    /**
     * 当天加入的新客流失数量 , 因为官方没有返回由系统自行统计,
     */
    private Integer newContactLossCnt ;

    /**
     * 获取新客留存率   流失客户数/ 新增客户数
     *
     * @return 新客留存率
     */
    public String getNewContactRetentionRate() {
        if (newContactCnt == null || newContactLossCnt == null) {
            return BigDecimal.ZERO.toPlainString();
        }
        BigDecimal percent = new BigDecimal(100);
        if(newContactCnt == 0) {
            return percent.toPlainString();
        }
        // 百分比
        BigDecimal newCntDecimal = new BigDecimal(newContactCnt);
        BigDecimal lossCntDecimal = new BigDecimal(newContactLossCnt);
        int scale = 2;
        // 计算留存率  新客数-流失数/新客数
        return  percent.subtract(lossCntDecimal
                               .multiply(percent)
                               .divide(newCntDecimal, scale, RoundingMode.HALF_UP)
                               .stripTrailingZeros()
                       )
                       .toPlainString();
    }

    /**
     * 设置群数据
     *
     * @param groupChatData 群数据
     */
    public void setGroupChatData(WePageCountDTO groupChatData) {
        this.setChatCnt(groupChatData.getChatCnt());
        this.setChatTotal(groupChatData.getChatTotal());
        this.setChatHasMsg(groupChatData.getChatHasMsg());
        this.setNewChatCnt(groupChatData.getNewChatCnt());
        this.setNewMemberCnt(groupChatData.getNewMemberCnt());
        this.setMemberTotal(groupChatData.getMemberTotal());
        this.setMsgTotal(groupChatData.getMsgTotal());
    }
}
