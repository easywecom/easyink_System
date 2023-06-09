package com.easyink.wecom.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.dtflys.forest.config.ForestConfiguration;
import com.dtflys.forest.exceptions.ForestRuntimeException;
import com.dtflys.forest.http.ForestRequest;
import com.dtflys.forest.http.ForestResponse;
import com.dtflys.forest.interceptor.Interceptor;
import com.dtflys.forest.utils.ForestDataType;
import com.easyink.common.config.WeComeConfig;
import com.easyink.common.constant.WeConstans;
import com.easyink.common.utils.spring.SpringUtils;
import com.easyink.wecom.domain.dto.WeResultDTO;
import com.easyink.wecom.service.WeAccessTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.PatternMatchUtils;

import java.util.Arrays;

/**
 * 类名: 自建应用AccessToken拦截器
 *
 * @author: 1*+
 * @date: 2021-12-27 10:46
 */
@Slf4j
@Component
public class WeAccessTokenInterceptor implements Interceptor<Object> {


    private final WeAccessTokenService weAccessTokenService;
    private final WeComeConfig weComeConfig;
    private final ForestConfiguration forestConfiguration;
    private final String urlPrefix;

    @Lazy
    public WeAccessTokenInterceptor(WeAccessTokenService weAccessTokenService, WeComeConfig weComeConfig) {
        this.weAccessTokenService = weAccessTokenService;
        this.weComeConfig = weComeConfig;
        forestConfiguration = SpringUtils.getBean(ForestConfiguration.class);
        String weComServerUrl = String.valueOf(forestConfiguration.getVariableValue(WeConstans.WECOM_SERVER_URL));
        String weComePrefix = String.valueOf(forestConfiguration.getVariableValue(WeConstans.WECOM_PREFIX));
        this.urlPrefix = weComServerUrl + weComePrefix;
    }


    /**
     * 该方法在请求发送之前被调用, 若返回false则不会继续发送请求
     */
    @Override
    public boolean beforeExecute(ForestRequest request) {
        String uri = request.getUrl().replace(urlPrefix, "");
        log.info(">>>>>>>>>>>>>>>>>>>>>>>>uri：{},query: {},body: {}", uri, request.getQueryString(), request.getBody());

        if (!Arrays.asList(weComeConfig.getFileUplodUrl()).contains(uri)) {
            request.setDataType(ForestDataType.JSON);
            request.setContentType("application/json");
        }
        // 添加请求参数access_token+
        if (PatternMatchUtils.simpleMatch(weComeConfig.getNoAccessTokenUrl(), uri)) {
            return true;
        }
        String token;
        String tokenKeyName = "access_token";
        String corpid = request.getHeaderValue("corpid");

        if (PatternMatchUtils.simpleMatch(weComeConfig.getNeedContactTokenUrl(), uri)) {
            //需要联系人token
            token = weAccessTokenService.findContactAccessToken(corpid);
        } else if (PatternMatchUtils.simpleMatch(weComeConfig.getNeedChatTokenUrl(), uri)) {
            //需要会话存档token
            token = weAccessTokenService.findChatAccessToken(corpid);
        } else if (PatternMatchUtils.simpleMatch(weComeConfig.getThirdAppUrl(), uri)) {
            //内部应用token
            String agentId = StrUtil.isEmpty(request.getHeaderValue(WeConstans.THIRD_APP_PARAM_TIP)) ?
                    (String) request.getQuery(WeConstans.THIRD_APP_PARAM_TIP) : request.getHeaderValue(WeConstans.THIRD_APP_PARAM_TIP);
            token = weAccessTokenService.findInternalAppAccessToken(agentId, corpid);
        } else if (PatternMatchUtils.simpleMatch(weComeConfig.getNeedCustomTokenUrl(), uri)) {
            //客服联系token
            token = weAccessTokenService.findCustomAccessToken(corpid);
        } else {
            token = weAccessTokenService.findCommonAccessToken(corpid);
        }
        request.addQuery(tokenKeyName, token);
        return true;
    }


    /**
     * 请求发送失败时被调用
     *
     * @param e
     * @param forestRequest
     * @param forestResponse
     */
    @Override
    public void onError(ForestRuntimeException e, ForestRequest forestRequest, ForestResponse forestResponse) {
        log.error("请求失败url:【{}】,result:【{}】", forestRequest.getUrl(), forestResponse.getContent());
    }

    @Override
    public void onRetry(ForestRequest request, ForestResponse response) {
        log.error("准备重试, url:【{}】,result:【{}】,retryCnt:【{}】", request.getUrl(), response.getContent() ,request.getCurrentRetryCount());
    }

    /**
     * 请求成功调用(微信端错误异常统一处理)
     *
     * @param o
     * @param forestRequest
     * @param forestResponse
     */
    @Override
    public void onSuccess(Object o, ForestRequest forestRequest, ForestResponse forestResponse) {
        log.info("url:【{}】,result:【{}】", forestRequest.getUrl(), forestResponse.getContent());
        WeResultDTO weResultDto = JSONUtil.toBean(forestResponse.getContent(), WeResultDTO.class);
        // 部分uri 错误码需要单独业务处理不抛出异常
        String uri = forestRequest.getUrl().replace(urlPrefix, "");
        if (PatternMatchUtils.simpleMatch(weComeConfig.getNeedErrcodeUrl(), uri)) {
            return;
        }
        if (null != weResultDto.getErrcode() && !WeConstans.WE_SUCCESS_CODE.equals(weResultDto.getErrcode()) && !WeConstans.NOT_EXIST_CONTACT.equals(weResultDto.getErrcode())) {
            throw new ForestRuntimeException(forestResponse.getContent());
        }

    }


}
