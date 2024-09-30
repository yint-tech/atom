package cn.iinti.atom.service.base.config;

import cn.iinti.atom.BuildInfo;
import cn.iinti.atom.service.base.alert.EventScript;
import cn.iinti.atom.service.base.env.Environment;
import cn.iinti.atom.utils.CommonUtils;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统设置,所有系统设置我们统一聚合在同一个文件中，避免默认值无法对齐
 */
public class Settings {

    public static JSONObject allSettingsVo() {
        JSONObject ret = new JSONObject();
        ret.put("normal", allSettings.stream().map(settingConfig ->
                        new JSONObject().fluentPut("key", settingConfig.getKey())
                                .fluentPut("value", settingConfig.valueVo())
                                .fluentPut("type", settingConfig.getSupplier().configType())
                                .fluentPut("desc", settingConfig.desc)
                                .fluentPut("detailDesc", settingConfig.detailDesc))
                .collect(Collectors.toList()));
        return ret;
    }

    private static final List<SettingConfig> allSettings = Lists.newArrayList();

    @SuppressWarnings("unused")
    private static Configs.BooleanConfigValue newBooleanConfig(String key, boolean defaultValue, String desc) {
        return newBooleanConfig(key, defaultValue, desc, desc);
    }

    private static Configs.BooleanConfigValue newBooleanConfig(String key, boolean defaultValue, String desc, String detailDesc) {
        Configs.BooleanConfigValue configValue = new Configs.BooleanConfigValue(key, defaultValue);
        allSettings.add(new SettingConfig(key, configValue, desc, detailDesc));
        return configValue;
    }

    @SuppressWarnings("unused")
    private static Configs.IntegerConfigValue newIntConfig(String key, int defaultValue, String desc) {
        return newIntConfig(key, defaultValue, desc, desc);
    }

    private static Configs.IntegerConfigValue newIntConfig(String key, int defaultValue, String desc, String detailDesc) {
        Configs.IntegerConfigValue configValue = new Configs.IntegerConfigValue(key, defaultValue);
        allSettings.add(new SettingConfig(key, configValue, desc, detailDesc));
        return configValue;
    }


    @SuppressWarnings("unused")
    private static Configs.StringConfigValue newStringConfig(String key, String defaultValue, String desc) {
        return newStringConfig(key, defaultValue, desc, desc);
    }

    private static Configs.StringConfigValue newStringConfig(String key, String defaultValue, String desc, String detailDesc) {
        Configs.StringConfigValue configValue = new Configs.StringConfigValue(key, defaultValue);
        allSettings.add(new SettingConfig(key, configValue, desc, detailDesc));
        return configValue;
    }

    @SuppressWarnings("unused")
    private static Configs.MultiLineStrConfigValue newMultilineStrConfig(String key, String defaultValue, String desc) {
        return newMultilineStrConfig(key, defaultValue, desc, desc);
    }

    private static Configs.MultiLineStrConfigValue newMultilineStrConfig(String key, String defaultValue, String desc, String detailDesc) {
        Configs.MultiLineStrConfigValue configValue = new Configs.MultiLineStrConfigValue(key, defaultValue);
        allSettings.add(new SettingConfig(key, configValue, desc, detailDesc));
        return configValue;
    }

    public static <T> Configs.ConfigValue<T> newCustomConfig(CustomConfigBuilder<T> builder) {
        Configs.ConfigValue<T> configValue = new Configs.ConfigValue<>(builder.configKey, builder.defaultValue) {
            @Override
            protected Configs.TransformFunc<T> transformer() {
                return builder.transformFunc;
            }

            @Override
            protected String configType() {
                return builder.configType;
            }
        };
        allSettings.add(new SettingConfig(builder.configKey, configValue, builder.desc, builder.detailDesc));
        return configValue;
    }

    @Builder
    public static class CustomConfigBuilder<T> {
        @NotBlank
        private String configKey;
        private T defaultValue;
        @NotBlank
        private String desc;
        private String detailDesc;
        private Configs.TransformFunc<T> transformFunc;
        private String configType;
    }

    @Getter
    @AllArgsConstructor
    public static class SettingConfig {
        private String key;
        private Configs.ConfigValue<?> supplier;
        private String desc;
        private String detailDesc;

        public Object valueVo() {
            return StringUtils.isNotBlank(supplier.sValue) ? supplier.sValue :
                    supplier.value;
        }
    }


    public static final Configs.BooleanConfigValue allowRegisterUser = newBooleanConfig(
            BuildInfo.appName + ".user.allowRegister", false, "是否允许注册用户",
            "设置不允许注册新用户，则可以避免用户空白注册，规避系统安全机制不完善，让敏感数据通过注册泄漏"
    );


    public static final Configs.StringConfigValue outIpTestUrl = newStringConfig(
            BuildInfo.appName + ".outIpTestUrl", "https://iinti.cn/conn/getPublicIp?scene=" + BuildInfo.appName,
            "出口ip探测URL", "计算当前服务器节点的出口IP，用于多节点部署在公网时多节点事件通讯"
    );


    public static final Configs.StringConfigValue systemNotice = newStringConfig(
            BuildInfo.appName + ".systemNotice", "",
            "系统通告信息", "在框架前端系统，将会在用户avatar推送消息"
    );

    public static final Configs.StringConfigValue docNotice = newStringConfig(
            BuildInfo.appName + ".docNotice", "",
            "文档首页通告信息", "在框架文档系统中，将会推送一段消息展示在文档中（此配置是html片段，故支持任意）"
    );

    public static Configs.ConfigValue<EventScript> eventNotifyScript = newCustomConfig(CustomConfigBuilder.<EventScript>builder()
            .configKey(BuildInfo.appName + ".eventNotifyScript")
            .transformFunc((value, type) -> {
                if (StringUtils.isNotBlank(value)) {
                    return EventScript.compileScript(value);
                }
                return null;

            })
            .desc("事件通知处理脚本")
            .detailDesc("内部事件，通过调用脚本的方式通知到外部系统")
            .configType("multiLine")
            .build()
    );


    /**
     * 目前不可以使用接口内部类和接口常量，目前这可能和因体的基础工具链冲突
     */
    public static class Storage {
        public static final File root = Environment.storageRoot;

        // 本地存储方案资源目录，如果用户没有配置任何云存储方案，那么系统默认是使用本地存储方案
        public static final File localStorage = CommonUtils.forceMkdir(new File(root, "storage"));
    }
}
