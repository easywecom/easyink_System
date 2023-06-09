package com.easyink.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 类名: 微信公开平台配置
 *
 * @author : silver_chariot
 * @date : 2022/7/19 18:04
 **/
@Component
@Data
@ConfigurationProperties(prefix = "wechatopen")
public class WechatOpenConfig {
    /**
     * 小程序配置
     */
    private MiniApp miniApp;
    /**
     * 公众号配置
     */
    private OfficialAccount officialAccount;
    /**
     * 第三方平台设置
     */
    private Platform3rdAccount platform3rdAccount;

    @Data
    public static class MiniApp {
        /**
         * 小程序appid
         */
        private String appId;
        /**
         * 小程序秘钥
         */
        private String appSecret;
        /**
         * 所属环境  要打开的小程序版本。正式版为 "release"，
         * 体验版为"trial"，开发版为"develop"，仅在微信外打开时生效。
         */
        private String envVersion;
        /**
         * 需要token的接口
         */
        private String[] needTokenUrl;
    }

    @Data
    public static class OfficialAccount {
        /**
         * 域名
         */
        private String domain;
        /**
         * appId
         */
        private String appId;
        /**
         * appSecret
         */
        private String appSecret;

        /**
         * 需要token的接口
         */
        private String[] needTokenUrl;
    }

    /**
     * 微信开放平台-第三方平台配置信息
     */
    @Data
    public static class Platform3rdAccount {
        /**
         * appid
         */
        private String appId;

        /**
         * appSecret
         */
        private String appSecret;

        /**
         * 公众号开发域名，中间页域名
         */
        private String domain;

        /**
         * 消息加解密Key
         */
        private String aesKey;

        /**
         *消息校验Token
         */
        private String componentToken;

        /**
         * 需要token的接口
         */
        private String[] needTokenUrl;

        /**
         * 授权发起页域名
         */
        private String authInitPageDomain;
    }


}
