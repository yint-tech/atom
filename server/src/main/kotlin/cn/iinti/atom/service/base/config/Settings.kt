package cn.iinti.atom.service.base.config

import cn.iinti.atom.BuildInfo
import cn.iinti.atom.service.base.alert.EventScript
import cn.iinti.atom.service.base.env.Environment
import cn.iinti.atom.utils.CommonUtils
import com.alibaba.fastjson.JSONObject
import com.google.common.collect.Lists
import jakarta.validation.constraints.NotBlank
import org.apache.commons.lang3.ClassUtils
import org.apache.commons.lang3.StringUtils
import java.io.File

/**
 * 系统设置,所有系统设置我们统一聚合在同一个文件中，避免默认值无法对齐
 */
object Settings {
    
    fun allSettingsVo(): JSONObject {
        val ret = JSONObject()
        ret["normal"] = allSettings.map { settingConfig ->
            JSONObject().fluentPut("key", settingConfig.key)
                .fluentPut("value", settingConfig.valueVo())
                .fluentPut("type", settingConfig.supplier.configType())
                .fluentPut("desc", settingConfig.desc)
                .fluentPut("detailDesc", settingConfig.detailDesc)
        }.toList()
        return ret
    }

    private val allSettings = Lists.newArrayList<SettingConfig>()

    private fun newBooleanConfig(
        key: String,
        defaultValue: Boolean,
        desc: String,
        detailDesc: String
    ): Configs.BooleanConfigValue {
        val configValue = Configs.BooleanConfigValue(key, defaultValue)
        allSettings.add(SettingConfig(key, configValue, desc, detailDesc))
        return configValue
    }

    private fun newIntConfig(
        key: String,
        defaultValue: Int,
        desc: String,
        detailDesc: String
    ): Configs.IntegerConfigValue {
        val configValue = Configs.IntegerConfigValue(key, defaultValue)
        allSettings.add(SettingConfig(key, configValue, desc, detailDesc))
        return configValue
    }

    private fun newStringConfig(
        key: String,
        defaultValue: String,
        desc: String,
        detailDesc: String
    ): Configs.StringConfigValue {
        val configValue = Configs.StringConfigValue(key, defaultValue)
        allSettings.add(SettingConfig(key, configValue, desc, detailDesc))
        return configValue
    }

    private fun newMultilineStrConfig(
        key: String,
        defaultValue: String,
        desc: String,
        detailDesc: String
    ): Configs.MultiLineStrConfigValue {
        val configValue = Configs.MultiLineStrConfigValue(key, defaultValue)
        allSettings.add(SettingConfig(key, configValue, desc, detailDesc))
        return configValue
    }

    
    fun <T> newCustomConfig(builder: CustomConfigBuilder<T>): Configs.ConfigValue<T> {
        val configValue = object : Configs.ConfigValue<T>(builder.configKey!!, builder.defaultValue!!) {
            override fun transformer(): Configs.TransformFunc<T?>? {
                return builder.transformFunc
            }

            override fun configType(): String {
                return builder.configType
            }
        }
        allSettings.add(SettingConfig(builder.configKey!!, configValue, builder.desc!!, builder.detailDesc!!))
        return configValue
    }

    data class CustomConfigBuilder<T>(
        @field:NotBlank
        var configKey: String? = null,
        var defaultValue: T? = null,
        @field:NotBlank
        var desc: String? = null,
        var detailDesc: String? = null,
        var transformFunc: Configs.TransformFunc<T?>? = null,
        var configType: String = ""
    )

    data class SettingConfig(
        val key: String,
        val supplier: Configs.ConfigValue<*>,
        val desc: String,
        val detailDesc: String
    ) {
        fun valueVo(): Any? {
            if (supplier.value != null && ClassUtils.isPrimitiveOrWrapper(supplier.value?.javaClass ?: return null)) {
                return supplier.value
            }
            return supplier.sValue
        }
    }

    @JvmField
    val allowRegisterUser = newBooleanConfig(
        "${BuildInfo.appName}.user.allowRegister", false, "是否允许注册用户",
        "设置不允许注册新用户，则可以避免用户空白注册，规避系统安全机制不完善，让敏感数据通过注册泄漏"
    )

    @JvmField
    val blockSwagger = newBooleanConfig(
        "${BuildInfo.appName}.user.blockSwagger", false, "拦截Swagger",
        "系统如果部署在公网，swagger将会展示接口信息，实例在政府机构内部部署等情况将会被安全扫组件判定为数据泄露"
    )

    @JvmField
    val blockActuator = newBooleanConfig(
        "${BuildInfo.appName}.user.blockActuator", false, "拦截Actuator",
        "系统如果部署在公网，Actuator可以导出监控指标数据，实例在政府机构内部部署等情况将会被安全扫组件判定为数据泄露"
    )

    
    val outIpTestUrl = newStringConfig(
        "${BuildInfo.appName}.outIpTestUrl", "https://iinti.cn/conn/getPublicIp?scene=${BuildInfo.appName}",
        "出口ip探测URL", "计算当前服务器节点的出口IP，用于多节点部署在公网时多节点事件通讯"
    )

    
    val systemNotice = newStringConfig(
        "${BuildInfo.appName}.systemNotice", "",
        "系统通告信息", "在框架前端系统，将会在用户avatar推送消息"
    )

    
    val docNotice = newStringConfig(
        "${BuildInfo.appName}.docNotice", "",
        "文档首页通告信息", "在框架文档系统中，将会推送一段消息展示在文档中（此配置是html片段，故支持任意）"
    )

    @JvmField
    val eventNotifyScript = newCustomConfig(
        CustomConfigBuilder<EventScript>().apply {
            configKey = "${BuildInfo.appName}.eventNotifyScript"
            transformFunc =
                Configs.TransformFunc { value, _ ->
                    if (StringUtils.isNotBlank(value)) {
                        EventScript.compileScript(value)
                    } else {
                        null
                    }
                }
            desc = "事件通知处理脚本"
            detailDesc = "内部事件，通过调用脚本的方式通知到外部系统"
            configType = "multiLine"
        }
    )

    /**
     * 目前不可以使用接口内部类和接口常量，目前这可能和因体的基础工具链冲突
     */
    object Storage {
        @JvmField
        val root = Environment.storageRoot

        // 本地存储方案资源目录，如果用户没有配置任何云存储方案，那么系统默认是使用本地存储方案
        val localStorage = CommonUtils.forceMkdir(File(root, "storage"))
    }
}