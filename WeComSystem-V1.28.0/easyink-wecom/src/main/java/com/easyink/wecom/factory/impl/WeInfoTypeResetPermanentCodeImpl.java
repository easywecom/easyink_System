package com.easyink.wecom.factory.impl;

import com.easyink.wecom.domain.vo.WxCpXmlMessageVO;
import com.easyink.wecom.factory.WeCallBackEventFactory;
import com.easyink.wecom.service.We3rdAppService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 类名: WeInfoTypeResetPermanentCodeImpl
 *
 * @author: 1*+
 * @date: 2021-09-09 14:23
 */
@Slf4j
@Service("reset_permanent_code")
public class WeInfoTypeResetPermanentCodeImpl implements WeCallBackEventFactory {

    private final We3rdAppService we3rdAppService;

    @Autowired
    public WeInfoTypeResetPermanentCodeImpl(We3rdAppService we3rdAppService) {
        this.we3rdAppService = we3rdAppService;
    }

    @Override
    public void eventHandle(WxCpXmlMessageVO message) {
        if (ObjectUtils.isEmpty(message)) {
            log.error("message为空");
            return;
        }
        if (StringUtils.isNotBlank(message.getAuthCode())) {
            we3rdAppService.resetPermanentCode(message.getAuthCode(), message.getSuiteId());
        }
    }
}
