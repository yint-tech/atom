package cn.iinti.atom.system;


import cn.iinti.atom.entity.UserInfo;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.BooleanUtils;

public class AppContext {
    private static final ThreadLocal<UserInfo> userInfoThreadLocal = new ThreadLocal<>();

    private static final ThreadLocal<Boolean> API_USER = new ThreadLocal<>();

    private static final ThreadLocal<LoginRequired> LOGIN_ANNOTATION = new ThreadLocal<>();
    private static final ThreadLocal<HttpServletResponse> SERVLET_RESPONSE = new ThreadLocal<>();

    public static UserInfo getUser() {
        return userInfoThreadLocal.get();
    }

    public static void setUser(UserInfo user) {
        userInfoThreadLocal.set(user);
    }

    public static void markApiUser() {
        API_USER.set(true);
    }

    public static void setLoginAnnotation(LoginRequired loginRequired) {
        LOGIN_ANNOTATION.set(loginRequired);
    }

    public static LoginRequired getLoginAnnotation() {
        return LOGIN_ANNOTATION.get();
    }

    public static HttpServletResponse getServletResponse() {
        return SERVLET_RESPONSE.get();
    }

    public static boolean isApiUser() {
        Boolean aBoolean = API_USER.get();
        return BooleanUtils.isTrue(aBoolean);
    }

    public static void removeUser() {
        userInfoThreadLocal.remove();
        API_USER.remove();
        LOGIN_ANNOTATION.remove();
        SERVLET_RESPONSE.remove();
    }
}
