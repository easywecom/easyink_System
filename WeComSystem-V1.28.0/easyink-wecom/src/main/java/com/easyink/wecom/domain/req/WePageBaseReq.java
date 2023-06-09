package com.easyink.wecom.domain.req;

import com.easyink.common.utils.bean.BeanUtils;
import com.easyink.wecom.domain.dto.transfer.TransferResignedGroupChatResp;
import com.easyink.wecom.domain.resp.WePageBaseResp;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 类名: 企微API基本分页请求实体
 *
 * @author : silver_chariot
 * @date : 2021/11/1 11:34
 */
@Data
public abstract class WePageBaseReq<T> {
    /**
     * 用于分页查询的游标，字符串类型，由上一次调用返回，首次调用可不填
     */
    private String cursor;

    protected WePageBaseReq() {
    }

    /**
     * 执行一次需要分页的企微API 请求, 实现类需要重写并调用相应的企微API
     *
     * @param corpId 企业ID
     * @return {@link WePageBaseResp}
     */
    public abstract WePageBaseResp<T> execute(String corpId);

    /**
     * 执行多次需要分页的企微API 请求 ,直到没有下一页
     *
     * @param corpId 企业ID
     * @return {@link WePageBaseResp} 接口响应
     */
    public WePageBaseResp<T> executeTillNoNextPage(String corpId)  {
        // 待返回的分页列表
        List<T> totalList = new ArrayList<>();
        // 接口单次的响应数据
        WePageBaseResp<T> onceResp = null;
        // 保存全部数据
        WePageBaseResp<T> respAll = null;
        // 至少有一次正确返回
        boolean resultSuccessFlag = false;
        do {
            setCursorIfExist(onceResp);
            onceResp = this.execute(corpId);
            if (onceResp == null || CollectionUtils.isEmpty(onceResp.getPageList())) {
                break;
            }
            // 其他参数只复制一次
            if(!resultSuccessFlag){
                respAll = onceResp;
                resultSuccessFlag = true;
            }
            totalList.addAll(onceResp.getPageList());
        } while (StringUtils.isNotBlank(onceResp.getNext_cursor()));
        if(respAll == null){
            return null;
        }
        respAll.setTotalList(totalList);
        return respAll;
    }

    /**
     * 设置游标
     *
     * @param resp {@link WePageBaseResp} 上一次的接口响应,如第一次可传null
     */
    @JsonIgnore
    public void setCursorIfExist(WePageBaseResp resp) {
        if (resp == null || StringUtils.isBlank(resp.getNext_cursor())) {
            return;
        }
        setCursor(resp.getNext_cursor());
    }


}
