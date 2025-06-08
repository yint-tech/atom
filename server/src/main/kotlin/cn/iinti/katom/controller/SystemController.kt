package cn.iinti.katom.controller

import cn.iinti.atom.BuildInfo
import cn.iinti.katom.entity.CommonRes
import cn.iinti.katom.base.BroadcastService
import cn.iinti.katom.base.config.Settings
import cn.iinti.katom.base.env.Environment
import cn.iinti.katom.utils.ServerIdentifier
import com.alibaba.fastjson.JSONObject
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(BuildInfo.restfulApiPrefix + "/system")
class SystemController {

    // todo，开源后需要考虑安全，此接口需要做内部鉴权
    @Operation(summary = "内部接口,获取当前设备的clientId")
    @GetMapping("/exchangeClientId")
    fun exchangeClientId(): CommonRes<String> {
        return CommonRes.success(ServerIdentifier.id())
    }

    // todo，开源后需要考虑安全，此接口需要做内部鉴权
    @Operation(summary = "内部接口，触发广播")
    @GetMapping("/triggerBroadcast")
    fun triggerBroadcast(@RequestParam topic: String): CommonRes<String> {
        return CommonRes.success(BroadcastService.callListener(topic))
    }

    @Operation(summary = "系统信息")
    @GetMapping("/systemInfo")
    fun systemInfo(): CommonRes<JSONObject> {
        return Environment.buildInfo()
    }

    @Operation(summary = "停机通知（软件更新或者升级前，通知业务模块做收尾工作）,返回当前pending任务数量，当数据为0则代表可以安全停机")
    @GetMapping("/prepareShutdown")
    fun prepareShutdown(): Int {
        return Environment.prepareShutdown()
    }

    @Operation(summary = "系统通告信息")
    @GetMapping("/systemNotice")
    fun systemNotice(): CommonRes<String?> {
        return CommonRes.success(Settings.systemNotice.value)
    }

    @Operation(summary = "文档首页通告信息")
    @GetMapping("/docNotice")
    fun docNotice(): CommonRes<String?> {
        return CommonRes.success(Settings.docNotice.value)
    }
}