package cn.iinti.katom.controller

import cn.iinti.atom.BuildInfo
import cn.iinti.katom.entity.CommonRes
import cn.iinti.katom.entity.UserInfo
import cn.iinti.katom.mapper.UserInfoMapper
import cn.iinti.katom.base.BroadcastService
import cn.iinti.katom.base.UserInfoService
import cn.iinti.katom.base.env.Environment
import cn.iinti.katom.base.perm.PermsService
import cn.iinti.katom.system.AppContext
import cn.iinti.katom.system.LoginRequired
import cn.iinti.katom.utils.ServletUtil
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import io.swagger.v3.oas.annotations.Operation
import jakarta.annotation.Resource
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.constraints.NotBlank
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping(BuildInfo.restfulApiPrefix + "/user-info")
class UserInfoController {

    @Resource
    lateinit var userInfoService: UserInfoService

    @Resource
    lateinit var userInfoMapper: UserInfoMapper

    @Resource
    lateinit var permsService: PermsService

    @Operation(summary = "登陆")
    @RequestMapping(value = ["/login"], method = [RequestMethod.POST])
    fun login(userName: String, password: String): CommonRes<UserInfo> {
        return userInfoService.login(userName, password)
    }

    @Operation(summary = "登陆")
    @RequestMapping(value = ["/getLogin"], method = [RequestMethod.GET])
    fun getLogin(userName: String, password: String): CommonRes<UserInfo> {
        return userInfoService.login(userName, password)
    }

    @Operation(summary = "通过一个确定url的登录，登录成功后重定向到根")
    @RequestMapping(value = ["/cookieLogin"], method = [RequestMethod.GET])
    fun cookieLogin(userName: String, password: String, httpServletResponse: HttpServletResponse) {
        val commonRes = userInfoService.login(userName, password)
        if (commonRes.isOk()) {
            val cookie = Cookie(BuildInfo.userLoginTokenKey, commonRes.data!!.loginToken)
            // cookie是一个临时的存储，我们只给他60s的有效时间
            cookie.maxAge = 60
            httpServletResponse.addCookie(cookie)
            httpServletResponse.sendRedirect("/")
            return
        }
        ServletUtil.writeRes(httpServletResponse, commonRes)
    }

    @Operation(summary = "注册")
    @RequestMapping(value = ["/register"], method = [RequestMethod.POST])
    fun register(userName: String, password: String): CommonRes<UserInfo> {
        return userInfoService.register(userName, password)
    }

    @LoginRequired(apiToken = true, skipLogRecord = true)
    @Operation(summary = "当前用户信息")
    @RequestMapping(value = ["/userInfo"], method = [RequestMethod.GET])
    fun userInfo(): CommonRes<UserInfo> {
        val user = AppContext.getUser()!!
        if (AppContext.isApiUser()) {
            // api方式无法获取到用户密码，我们认为API是用在代码中，他是低优账户体系。后台账户将会对他保密
            user.password = null
        }
        return CommonRes.success(user)
    }

    @LoginRequired
    @Operation(summary = "刷新当前用户token")
    @GetMapping(value = ["/refreshToken"])
    fun refreshToken(): CommonRes<String> {
        val newToken = userInfoService.refreshToken(AppContext.getUser()!!.loginToken)
        if (newToken == null) {
            return CommonRes.failed(CommonRes.statusLoginExpire, "请重新登陆")
        }
        return CommonRes.success(newToken)
    }

    @LoginRequired
    @Operation(summary = "重置密码")
    @PostMapping(value = ["/resetPassword"])
    fun resetPassword(newPassword: String): CommonRes<UserInfo> {
        val mUser = AppContext.getUser()
        if (mUser!!.isAdmin!! && Environment.isDemoSite) {
            return CommonRes.failed("测试demo网站不允许修改管理员密码")
        }
        return userInfoService.resetUserPassword(mUser.id, newPassword)
    }

    @LoginRequired
    @Operation(summary = "重新生产api访问的token")
    @GetMapping("/regenerateAPIToken")
    fun regenerateAPIToken(): CommonRes<UserInfo> {
        val mUser = AppContext.getUser()!!
        mUser.apiToken = UUID.randomUUID().toString()
        userInfoMapper.update(
            null, UpdateWrapper<UserInfo>()
                .eq(UserInfo.USER_NAME, mUser.userName)
                .set(UserInfo.API_TOKEN, mUser.apiToken)
        )
        BroadcastService.triggerEvent(BroadcastService.Topic.USER)
        return CommonRes.success(mUser)
    }

    @Operation(summary = "给用户编辑权限")
    @LoginRequired(forAdmin = true)
    @PostMapping("/editUserPerm")
    fun editUserPerm(@NotBlank userName: String, permsConfig: String): CommonRes<UserInfo> {
        return userInfoService.editUserPerm(userName, permsConfig)
    }

    @Operation(summary = "所有的权限类型")
    @LoginRequired(forAdmin = true)
    @GetMapping("/permScopes")
    fun permScopes(): CommonRes<List<String>> {
        return CommonRes.success(permsService.permissionScopes())
    }

    @Operation(summary = "某个作用域下，权限授权范围枚举项")
    @LoginRequired(forAdmin = true)
    @GetMapping("/permItemsOfScope")
    fun permItemsOfScope(scope: String): CommonRes<List<String>> {
        return permsService.perms(scope)
    }
}