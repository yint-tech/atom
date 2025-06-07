package cn.iinti.atom.controller

import cn.iinti.atom.BuildInfo
import cn.iinti.atom.entity.*
import cn.iinti.atom.mapper.ServerNodeMapper
import cn.iinti.atom.mapper.SysLogMapper
import cn.iinti.atom.mapper.UserInfoMapper
import cn.iinti.atom.service.base.BroadcastService
import cn.iinti.atom.service.base.UserInfoService
import cn.iinti.atom.service.base.alert.EventNotifierService
import cn.iinti.atom.service.base.config.ConfigService
import cn.iinti.atom.service.base.config.Settings
import cn.iinti.atom.service.base.config.SettingsValidate
import cn.iinti.atom.service.base.env.Environment
import cn.iinti.atom.system.LoginRequired
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.metadata.IPage
import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import io.swagger.v3.oas.annotations.Operation
import jakarta.annotation.Resource
import org.apache.commons.lang3.StringUtils
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping(BuildInfo.restfulApiPrefix + "/admin-op")
class AdminController {

    @Resource
    lateinit var userInfoService: UserInfoService

    @Resource
    lateinit var userInfoMapper: UserInfoMapper

    @Resource
    lateinit var configService: ConfigService

    @Resource
    lateinit var serverNodeMapper: ServerNodeMapper

    @Resource
    lateinit var sysLogMapper: SysLogMapper

    @Resource
    lateinit var eventNotifierService: EventNotifierService

    @Operation(summary = "创建用户")
    @LoginRequired(forAdmin = true, alert = true)
    @GetMapping("/createUser")
    fun createUser(userName: String, password: String): CommonRes<UserInfo> {
        return userInfoService.register(userName, password)
    }

    @Operation(summary = "将一个用户升级为管理员")
    @LoginRequired(forAdmin = true, alert = true)
    @GetMapping("/grantAdmin")
    fun grantAdmin(userName: String, isAdmin: Boolean): CommonRes<String> {
        if (StringUtils.isBlank(userName)) {
            return CommonRes.failed("没有用户名")
        }
        if (isAdmin && Environment.isDemoSite) {
            return CommonRes.failed("测试demo网站不允许设置新的管理员")
        }
        return userInfoService.grantAdmin(userName, isAdmin)
    }

    @Operation(summary = "系统设置项配置模版")
    @GetMapping("/settingTemplate")
    @LoginRequired(forAdmin = true)
    fun settingTemplate(): CommonRes<*> {
        return CommonRes.success(Settings.allSettingsVo())
    }

    @Operation(summary = "所有的系统配置")
    @GetMapping("/allConfig")
    @LoginRequired(forAdmin = true)
    fun allConfig(): CommonRes<List<SysConfig>> {
        return configService.allConfig()
    }

    @Operation(summary = "修改系统配置,批量")
    @PostMapping("/setConfigs")
    @LoginRequired(forAdmin = true, alert = true)
    fun setConfigs(@RequestBody configs: Map<String, String>): CommonRes<String> {
        if (Environment.isDemoSite) {
            return CommonRes.failed("测试demo网站不允许修改配置")
        }
        var errorMsg: String? = null
        for ((key, value) in configs) {
            val msg = SettingsValidate.doValidate(key, value)
            if (msg != null) {
                errorMsg = "error config: $key) $msg)"
                break
            }
        }
        if (StringUtils.isNotBlank(errorMsg)) {
            return CommonRes.failed(errorMsg!!)
        }

        if (configs.isEmpty()) {
            return CommonRes.failed("no config passed")
        }

        for ((key, value) in configs) {
            val res = configService.setConfig(key, value)
            if (!res.isOk()) {
                return res.errorTransfer()
            }
        }
        return CommonRes.success("ok")
    }

    @Operation(summary = "修改系统配置")
    @PostMapping("/setConfig")
    @LoginRequired(forAdmin = true, alert = true)
    fun setConfig(@RequestBody data: Map<String?, String?>): CommonRes<SysConfig> {
        if (Environment.isDemoSite) {
            return CommonRes.failed("测试demo网站不允许修改配置")
        }
        val key = data["key"]
        val value = data["value"]
        val msg = SettingsValidate.doValidate(key, value)
        if (StringUtils.isNotBlank(msg)) {
            return CommonRes.failed(msg!!)
        }
        val ret = configService.setConfig(key!!, value!!)
        BroadcastService.triggerEvent(BroadcastService.Topic.CONFIG)
        return ret
    }

    @Operation(summary = "管理员穿越到普通用户，获取普通用户token")
    @LoginRequired(forAdmin = true)
    @GetMapping("/travelToUser")
    fun travelToUser(id: Long): CommonRes<UserInfo> {
        val toUser = userInfoMapper.selectById(id) ?: return CommonRes.failed("user not exist")
        toUser.loginToken = userInfoService.genLoginToken(toUser, LocalDateTime.now())
        return CommonRes.success(toUser)
    }

    @Operation(summary = "用户列表")
    @LoginRequired(forAdmin = true)
    @GetMapping("/listUser")
    fun listUser(page: Int, pageSize: Int): CommonRes<IPage<UserInfo>> {
        var actualPage = page
        if (actualPage < 1) {
            actualPage = 1
        }
        return CommonRes.success(
            userInfoMapper.selectPage(
                Page(actualPage.toLong(), pageSize.toLong()),
                QueryWrapper()
            )
        )
    }

    @Operation(summary = "列出 server")
    @LoginRequired(forAdmin = true)
    @GetMapping("/listServer")
    fun listServer(page: Int, pageSize: Int): CommonRes<IPage<ServerNode>> {
        var actualPage = page
        if (actualPage < 1) {
            actualPage = 1
        }
        val queryWrapper = QueryWrapper<ServerNode>().orderByDesc(ServerNode.LAST_ACTIVE_TIME)
        return CommonRes.success(
            serverNodeMapper.selectPage(
                Page(actualPage.toLong(), pageSize.toLong()),
                queryWrapper
            )
        )
    }

    @Operation(summary = "设置服务器启用状态")
    @GetMapping("setServerStatus")
    @LoginRequired(forAdmin = true, alert = true)
    fun setServerStatus(id: Long, enable: Boolean?): CommonRes<ServerNode> {
        return CommonRes.ofPresent(serverNodeMapper.selectById(id))
            .ifOk { serverNode ->
                serverNode.enable = enable
                serverNodeMapper.updateById(serverNode)
            }
    }

    @Operation(summary = "查询操作日志")
    @GetMapping("/listSystemLog")
    @LoginRequired(forAdmin = true)
    fun listSystemLog(username: String?, operation: String?, page: Int, pageSize: Int): CommonRes<IPage<SysLog>> {
        val queryWrapper = QueryWrapper<SysLog>().apply {
            if (!username.isNullOrBlank()) eq(SysLog.USERNAME, username)
            if (!operation.isNullOrBlank()) eq(SysLog.OPERATION, operation)
            orderByDesc(SysLog.ID)
        }
        return CommonRes.success(sysLogMapper.selectPage(Page(page.toLong(), pageSize.toLong()), queryWrapper))
    }

    @Operation(summary = "触发指标告警事件任务")
    @GetMapping("/triggerMetricEvent")
    @LoginRequired(forAdmin = true, apiToken = true)
    fun triggerMetricEvent(): CommonRes<String> {
        eventNotifierService.scheduleMetricEvent(true)
        return CommonRes.success("ok")
    }
}