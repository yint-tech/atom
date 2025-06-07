package cn.iinti.atom.system

import cn.iinti.atom.BuildInfo
import cn.iinti.atom.entity.CommonRes.Companion.failed
import cn.iinti.atom.service.base.UserInfoService
import cn.iinti.atom.system.AppContext.markApiUser
import cn.iinti.atom.system.AppContext.removeUser
import cn.iinti.atom.system.AppContext.setLoginAnnotation
import cn.iinti.atom.system.AppContext.setUser
import com.alibaba.fastjson.JSONObject
import com.google.common.base.Charsets
import com.google.common.collect.Lists
import jakarta.annotation.Resource
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView
import java.io.IOException
import java.util.*

@Component
class LoginInterceptor : HandlerInterceptor {
    @Resource
    private val userInfoService: UserInfoService? = null

    private fun collectTokenList(request: HttpServletRequest): List<String> {
        val tokenList: MutableList<String> = Lists.newArrayList()

        // header 不区分大小写
        var operatorToken = request.getHeader(BuildInfo.userLoginTokenKey)
        if (StringUtils.isNotBlank(operatorToken)) {
            tokenList.add(operatorToken)
        }
        operatorToken = request.getParameter(BuildInfo.userLoginTokenKey)
        if (StringUtils.isNotBlank(operatorToken)) {
            tokenList.add(operatorToken)
        }

        val cookies = request.cookies
        if (cookies != null) {
            for (cookie in cookies) {
                if (cookie.name == BuildInfo.userLoginTokenKey) {
                    tokenList.add(cookie.value)
                }
            }
        }
        return tokenList.stream()
            .filter { s: String? ->
                StringUtils.isNotBlank(s) &&  // undefined,null as blank from frontend javascript
                        !StringUtils.containsAny(s, "undefined", "null")
            }
            .toList()
    }

    @Throws(IOException::class)
    private fun handleNoToken(loginRequired: LoginRequired?, response: HttpServletResponse): Boolean {
        // 不存在鉴权token
        if (loginRequired == null) {
            //不需要登陆
            return true
        }
        // 需要登录，但是没有token
        response.addHeader("content-type", "application/json; charset=utf-8")
        response.outputStream.write(needLoginResponse)
        return false
    }

    @Throws(Exception::class)
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (handler !is HandlerMethod) {
            return true
        }

        val method = handler.method
        val loginRequired = method.getAnnotation(LoginRequired::class.java)
        val tokenList = collectTokenList(request)
        if (tokenList.isEmpty()) {
            return handleNoToken(loginRequired, response)
        }

        var result = userInfoService!!.checkLogin(tokenList)
        if (loginRequired == null) {
            // no need login
            if (result.isOk()) {
                // but user send userToken
                setUser(result.data)
            }
            return true
        }

        if (!result.isOk()) {
            //如果这个接口允许api token访问，那么直接允许，并且使用对应的token所在账户身份
            if (loginRequired.apiToken) {
                result = userInfoService.checkAPIToken(tokenList)
            }
            if (!result.isOk()) {
                response.addHeader("content-type", "application/json; charset=utf-8")
                response.outputStream.write(loginExpire)
                return false
            }
            markApiUser()
        }
        if (loginRequired.forAdmin && BooleanUtils.isNotTrue(result.data!!.isAdmin)) {
            response.addHeader("content-type", "application/json; charset=utf-8")
            response.outputStream.write(onlyForAdminResponse)
            return false
        }
        setUser(result.data)
        setLoginAnnotation(loginRequired)
        return true
    }

    override fun postHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        modelAndView: ModelAndView?
    ) {
        removeUser()
    }

    companion object {
        private val needLoginResponse = JSONObject.toJSONString(failed<Any>("请登录后访问")).toByteArray(Charsets.UTF_8)
        private val loginExpire = JSONObject.toJSONString(failed<Any>("请重新登录")).toByteArray(Charsets.UTF_8)
        private val onlyForAdminResponse = JSONObject.toJSONString(failed<Any>("非管理员")).toByteArray(Charsets.UTF_8)
    }
}