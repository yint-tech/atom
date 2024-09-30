package cn.iinti.atom.system;


import cn.iinti.atom.BuildInfo;
import cn.iinti.atom.entity.CommonRes;
import cn.iinti.atom.entity.UserInfo;
import cn.iinti.atom.service.base.UserInfoService;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.lang.reflect.Method;
import java.util.List;


@Component
public class LoginInterceptor implements HandlerInterceptor {
    @Resource
    private UserInfoService userInfoService;

    private static final byte[] needLoginResponse = JSONObject.toJSONString(CommonRes.failed("请登录后访问")).getBytes(Charsets.UTF_8);
    private static final byte[] loginExpire = JSONObject.toJSONString(CommonRes.failed("请重新登录")).getBytes(Charsets.UTF_8);
    private static final byte[] onlyForAdminResponse = JSONObject.toJSONString(CommonRes.failed("非管理员")).getBytes(Charsets.UTF_8);

    @Override
    public boolean preHandle(@Nullable HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        if (request == null || response == null) {
            return true;
        }

        Method method = ((HandlerMethod) handler).getMethod();
        LoginRequired loginRequired = method.getAnnotation(LoginRequired.class);

        //不需要登陆
        if (loginRequired == null) {
            return true;
        }

        List<String> tokenList = Lists.newArrayList();

        // header 不区分大小写
        String operatorToken = request.getHeader(BuildInfo.userLoginTokenKey);
        if (StringUtils.isNotBlank(operatorToken)) {
            tokenList.add(operatorToken);
        }
        operatorToken = request.getParameter(BuildInfo.userLoginTokenKey);
        if (StringUtils.isNotBlank(operatorToken)) {
            tokenList.add(operatorToken);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(BuildInfo.userLoginTokenKey)) {
                    tokenList.add(cookie.getValue());
                }
            }
        }

        if (tokenList.isEmpty()) {
            response.addHeader("content-type", "application/json; charset=utf-8");
            response.getOutputStream().write(needLoginResponse);
            return false;
        }

        CommonRes<UserInfo> result = userInfoService.checkLogin(tokenList);
        if (!result.isOk()) {
            //如果这个接口允许api token访问，那么直接允许，并且使用对应的token所在账户身份
            if (loginRequired.apiToken()) {
                result = userInfoService.checkAPIToken(tokenList);
            }
            if (!result.isOk()) {
                response.addHeader("content-type", "application/json; charset=utf-8");
                response.getOutputStream().write(loginExpire);
                return false;
            }
            AppContext.markApiUser();
        }
        if (loginRequired.forAdmin()) {
            if (!BooleanUtils.isTrue(result.getData().getIsAdmin())) {
                response.addHeader("content-type", "application/json; charset=utf-8");
                response.getOutputStream().write(onlyForAdminResponse);
                return false;
            }
        }
        AppContext.setUser(result.getData());
        AppContext.setLoginAnnotation(loginRequired);
        return true;
    }

    @Override
    public void postHandle(@Nullable HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable Object handler, ModelAndView modelAndView) {
        AppContext.removeUser();
    }
}
