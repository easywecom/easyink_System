package com.easyink.wecom.login.util;

import com.easyink.common.constant.Constants;
import com.easyink.common.constant.RedisKeyConstants;
import com.easyink.common.core.domain.entity.WeCorpAccount;
import com.easyink.common.core.domain.model.LoginUser;
import com.easyink.common.core.redis.RedisCache;
import com.easyink.common.enums.LogoutReasonEnum;
import com.easyink.common.exception.CustomException;
import com.easyink.common.exception.user.NoLoginTokenException;
import com.easyink.common.token.SysPermissionService;
import com.easyink.common.token.TokenService;
import com.easyink.common.utils.ServletUtils;
import com.easyink.common.utils.spring.SpringUtils;
import com.easyink.wecom.service.WeCorpAccountService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * 类名: 登录用户工具
 *
 * @author : silver_chariot
 * @date : 2021/8/30 12:44
 */
@Component
@Slf4j
public class LoginTokenService {
    private LoginTokenService() {
    }

    /**
     * 获取当前登录用户 (只有带TOKEN的主动请求可获取)
     *
     * @return 当前登录用户
     * @throws NoLoginTokenException 不存在登录TOKEN异常
     */
    public static LoginUser getLoginUser() {
        LoginUser loginUser;
        try {
            loginUser = SpringUtils.getBean(TokenService.class).getLoginUser(ServletUtils.getRequest());
        } catch (Exception e) {
            throw new NoLoginTokenException("获取TOKEN失败");
        }
        if (null == loginUser) {
            throw new NoLoginTokenException("获取登录用户失败");
        }
        return loginUser;
    }

    /**
     * 根据token 获取当期那登录用户
     *
     * @param token token
     * @return 当前登录用户
     */
    public static LoginUser getLoginUserByToken(String token) {
        if (StringUtils.isBlank(token)) {
            throw new NoLoginTokenException("TOKEN为空");
        }
        TokenService tokenService = SpringUtils.getBean(TokenService.class);
        String userKey = tokenService.getUserKey(token);
        LoginUser loginUser = tokenService.getLoginUserByUserKey(userKey);
        if (loginUser == null) {
            throw new NoLoginTokenException("获取登录用户失败");
        }
        return loginUser;
    }

    /**
     * 刷新缓存中的用户信息（角色、数据权限)
     * 用于后台更新角色的权限、部门后及时刷新缓存中权限信息
     *
     * @throws NoLoginTokenException 不存在登录TOKEN异常
     */
    public static void refreshDataScope() {
        LoginUser loginUser = getLoginUser();
        refreshDataScope(loginUser);
    }

    /**
     * 刷新缓存中的用户信息（角色、数据权限)
     * 用于后台更新角色的权限、部门后及时刷新缓存中权限信息
     *
     * @param loginUser 当前登录用户
     */
    public static void refreshDataScope(LoginUser loginUser) {
        if (loginUser != null) {
            //补偿admin帐号没有corpid的情况
            if (loginUser.isSuperAdmin() && org.apache.commons.lang3.StringUtils.isBlank(loginUser.getCorpId())) {
                WeCorpAccountService weCorpAccountService = SpringUtils.getBean(WeCorpAccountService.class);
                WeCorpAccount weCorpAccount = weCorpAccountService.findValidWeCorpAccount();
                if (weCorpAccount != null && org.apache.commons.lang3.StringUtils.isNotBlank(weCorpAccount.getCorpId())) {
                    loginUser.getUser().setCorpId(weCorpAccount.getCorpId());
                }
            }
            loginUser.setPermissions(SpringUtils.getBean(SysPermissionService.class).getMenuPermission(loginUser));
            SpringUtils.getBean(TokenService.class).setLoginUser(loginUser);
        }
    }


    /**
     * 根据登录用户的REDIS KEY去刷新、重置登录用户的可视数据权限范围
     *
     * @param userKey 登录用户的REDIS KEY
     */
    public static void refreshDataScope(String userKey) {
        RedisCache redisCache = SpringUtils.getBean("redisCache", RedisCache.class);
        TokenService tokenService = SpringUtils.getBean(TokenService.class);
        LoginUser loginUser = tokenService.getLoginUserByUserKey(userKey);
        if (null != loginUser) {
            SpringUtils.getBean(SysPermissionService.class).setRoleAndDepartmentDataScope(loginUser);
            redisCache.setCacheObject(userKey, loginUser, tokenService.getExpireTime(), TimeUnit.MINUTES);
        }
    }



    /**
     * 强退某个公司除当前登录操作者外的所有后台登录账号
     *
     * @param corpId 公司ID
     */
    public static void forceToOffline(String corpId) {
        if (StringUtils.isBlank(corpId)) {
            return;
        }
        // 获取当前登录用户的redisKey
        TokenService tokenService = SpringUtils.getBean(TokenService.class);
        String currentUserKey = tokenService.getUserKey(ServletUtils.getRequest());
        RedisCache redisCache = SpringUtils.getBean("redisCache", RedisCache.class);
        // 获取缓存中所有登录用户
        Collection<String> keys = redisCache.keys(Constants.LOGIN_TOKEN_KEY + "*");
        for (String key : keys) {
            if (StringUtils.isBlank(key)) {
                continue;
            }
            try {
                LoginUser user = redisCache.getCacheObject(key);
                // 删除非当前操作用户的其他用户redis缓存
                if (corpId.equals(user.getCorpId()) && !key.equals(currentUserKey)) {
                    redisCache.deleteObject(key);
                    // 缓存登出原因30分钟
                    key = key.replaceAll(Constants.LOGIN_TOKEN_KEY, "");
                    redisCache.setCacheObject(RedisKeyConstants.ACCOUNT_LOGOUT_REASON_KEY + key, LogoutReasonEnum.CORP_ID_CHANGED.getCode()
                            , 30, TimeUnit.MINUTES);
                }
            } catch (CustomException e) {
                //此处捕获获取corpId抛出的异常
            }
        }
    }

    /**
     * 修改当前登录用户的CorpId 并刷新登录用户信息
     *
     * @param corpId 需要修改的corpId
     * @throws NoLoginTokenException 当前登录用户不存在时抛出该异常
     */
    public static void changeCorpId(String corpId) {
        if (StringUtils.isBlank(corpId)) {
            return;
        }
        // 1. 修改当前用户的corpId 并保存
        LoginUser loginUser = getLoginUser();
        loginUser.setCorpId(corpId);
        RedisCache redisCache = SpringUtils.getBean("redisCache", RedisCache.class);
        String currentUserKey = SpringUtils.getBean(TokenService.class).getUserKey(ServletUtils.getRequest());
        redisCache.setCacheObject(currentUserKey, loginUser);
        // 2. 修改后刷新当前的部门范围和菜单权限
        refreshDataScope();
    }

    /**
     * 修改当前登录用户的企微用户属性,(userId,头像等)
     *
     * @param loginUser 当前登录用户 {@link LoginUser}
     */
    public static void refreshWeUser(LoginUser loginUser) {
        if (loginUser == null || loginUser.getWeUser() == null || StringUtils.isBlank(loginUser.getWeUser().getUserId())) {
            return;
        }
        loginUser.setLoginTime(System.currentTimeMillis());
        TokenService tokenService = SpringUtils.getBean(TokenService.class);
        String currentUserKey = tokenService.getUserKey(loginUser.getToken());
        loginUser.setExpireTime(loginUser.getLoginTime() + tokenService.getExpireTime() * TokenService.MILLIS_MINUTE);
        // 获取当前登录用户
        RedisCache redisCache = SpringUtils.getBean("redisCache", RedisCache.class);
        redisCache.setCacheObject(currentUserKey, loginUser, tokenService.getExpireTime(), TimeUnit.MINUTES);
    }

    /**
     * 获取当前登录账号的用户名
     *
     * @return 当前登录账号的用户名
     */
    public static String getUsername() {
        return getLoginUser().getUsername();
    }
}