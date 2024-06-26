package cn.iinti.atom.controller;

import cn.iinti.atom.BuildInfo;
import cn.iinti.atom.entity.CommonRes;
import cn.iinti.atom.service.base.BroadcastService;
import cn.iinti.atom.service.base.config.Settings;
import cn.iinti.atom.service.base.env.Environment;
import cn.iinti.atom.utils.ServerIdentifier;
import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(BuildInfo.restfulApiPrefix + "/system")
public class SystemController {

    // todo，开源后需要考虑安全，此接口需要做内部鉴权
    @Operation(summary = "内部接口,获取当前设备的clientId")
    @GetMapping("/exchangeClientId")
    public CommonRes<String> exchangeClientId() {
        return CommonRes.success(ServerIdentifier.id());
    }

    // todo，开源后需要考虑安全，此接口需要做内部鉴权
    @Operation(summary = "内部接口，触发广播")
    @GetMapping("/triggerBroadcast")
    public CommonRes<String> triggerBroadcast(String topic) {
        return CommonRes.success(BroadcastService.callListener(topic));
    }


    @Operation(summary = "系统信息")
    @GetMapping("/systemInfo")
    public CommonRes<JSONObject> systemInfo() {
        return Environment.buildInfo();
    }

    @Operation(summary = "停机通知（软件更新或者升级前，通知业务模块做收尾工作）,返回当前pending任务数量，当数据为0则代表可以安全停机")
    @GetMapping("/prepareShutdown")
    public Integer prepareShutdown() {
        return Environment.prepareShutdown();
    }

    @Operation(summary = "系统通告信息")
    @GetMapping("/systemNotice")
    public CommonRes<String> systemNotice() {
        return CommonRes.success(Settings.systemNotice.value);
    }

    @Operation(summary = "文档首页通告信息")
    @GetMapping("/docNotice")
    public CommonRes<String> docNotice() {
        return CommonRes.success(Settings.docNotice.value);
    }
}
