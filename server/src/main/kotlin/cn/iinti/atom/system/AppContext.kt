package cn.iinti.atom.system

import cn.iinti.atom.entity.UserInfo
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.lang3.BooleanUtils

object AppContext {
    private val userInfoThreadLocal = ThreadLocal<UserInfo>()
    private val API_USER = ThreadLocal<Boolean>()
    private val LOGIN_ANNOTATION = ThreadLocal<LoginRequired>()
    private val SERVLET_RESPONSE = ThreadLocal<HttpServletResponse>()

    @JvmStatic
    fun getUser(): UserInfo {
        return userInfoThreadLocal.get()
    }

    fun setUser(user: UserInfo?) {
        userInfoThreadLocal.set(user)
    }

    fun markApiUser() {
        API_USER.set(true)
    }

    fun setLoginAnnotation(loginRequired: LoginRequired?) {
        LOGIN_ANNOTATION.set(loginRequired)
    }

    fun getLoginAnnotation(): LoginRequired? {
        return LOGIN_ANNOTATION.get()
    }

    fun getServletResponse(): HttpServletResponse? {
        return SERVLET_RESPONSE.get()
    }

    fun isApiUser(): Boolean {
        val aBoolean = API_USER.get()
        return BooleanUtils.isTrue(aBoolean)
    }

    fun removeUser() {
        userInfoThreadLocal.remove()
        API_USER.remove()
        LOGIN_ANNOTATION.remove()
        SERVLET_RESPONSE.remove()
    }
}