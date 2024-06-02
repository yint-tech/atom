package cn.iinti.atom.service.base.env;

import cn.iinti.atom.BuildInfo;
import cn.iinti.atom.entity.CommonRes;
import cn.iinti.atom.service.base.config.Configs;
import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
public class Environment {
    public static String APPLICATION_PROPERTIES = "application.properties";


    public static int tomcatPort;
    public static final boolean isLocalDebug =
            BooleanUtils.isTrue(Boolean.valueOf(Configs.getConfig("env.localDebug", "false")));
    public static final boolean isDemoSite =
            BooleanUtils.isTrue(Boolean.valueOf(Configs.getConfig("env.demoSite", "false")));

    public static final File runtimeClassPathDir = resolveClassPathDir();

    public static final boolean isIdeDevelopment = !runtimeClassPathDir.getName().equals("conf");


    public static CommonRes<JSONObject> buildInfo() {
        return CommonRes.success(new JSONObject()
                .fluentPut("buildInfo",
                        new JSONObject()
                                .fluentPut("versionCode", BuildInfo.versionCode)
                                .fluentPut("versionName", BuildInfo.versionName)
                                .fluentPut("buildTime", BuildInfo.buildTime)
                                .fluentPut("buildUser", BuildInfo.buildUser)
                                .fluentPut("gitId", GitProperties.GIT_ID.value)
                ).fluentPut("env",
                        new JSONObject()
                                .fluentPut("demoSite", isDemoSite)
                                .fluentPut("debug", isLocalDebug)
                )

        );
    }

    @Getter
    private static ApplicationContext app;


    public static void setupApp(WebServerInitializedEvent event) {
        app = event.getApplicationContext();
        tomcatPort = event.getWebServer().getPort();
    }

    @SneakyThrows
    public static void upgradeIfNeed(DataSource dataSource) {
        upgradeRuleHolders.sort(Comparator.comparingInt(o -> o.fromVersionCode));
        doDbUpGradeTask(dataSource);

        if (isIdeDevelopment) {
            // 本地代码执行模式，认为一定时最新版本，不需要执行升级代码
            return;
        }
        doLocalUpGradeTask(new File(runtimeClassPathDir, "versionCode.txt"));
        System.out.println("app: " + BuildInfo.appName + " version:(" + BuildInfo.versionCode + ":" + BuildInfo.versionName + ") buildTime:" + BuildInfo.buildTime);
    }

    @SuppressWarnings("all")
    private static final String DB_VERSION_SQL =
            "select config_value from sys_config where config_key='_atom_framework_version' and config_comment='_atom_framework'";

    @SuppressWarnings("all")
    private static final String UPDATE_DB_VERSION_SQL =
            "insert into sys_config (`config_comment`,`config_key`,`config_value`) values ('_atom_framework','_atom_framework_version','" + BuildInfo.versionCode + "') " +
                    "on duplicate key update `config_value`='" + BuildInfo.versionCode + "'";

    private static void doDbUpGradeTask(DataSource dataSource) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            // fetch preVersion
            try (Statement statement = conn.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery(DB_VERSION_SQL)) {
                    if (resultSet.next()) {
                        int preVersionCode = Integer.parseInt(resultSet.getString(1));
                        for (UpgradeRuleHolder upgradeRuleHolder : upgradeRuleHolders) {
                            if (upgradeRuleHolder.fromVersionCode < preVersionCode) {
                                continue;
                            }
                            System.out.println("db upgrade app from: " + upgradeRuleHolder.fromVersionCode + " to: " + upgradeRuleHolder.toVersionCode);
                            upgradeRuleHolder.upgradeHandler.doDbUpgrade(dataSource);
                            preVersionCode = upgradeRuleHolder.toVersionCode;
                        }
                    }
                }
            }

            // flush now version
            try (Statement statement = conn.createStatement()) {
                statement.execute(UPDATE_DB_VERSION_SQL);
            }
        }
    }

    private static void doLocalUpGradeTask(File versionCodeFile) throws IOException {
        if (versionCodeFile.exists()) {
            int preVersionCode = Integer.parseInt(FileUtils.readFileToString(versionCodeFile, StandardCharsets.UTF_8));

            for (UpgradeRuleHolder upgradeRuleHolder : upgradeRuleHolders) {
                if (upgradeRuleHolder.fromVersionCode < preVersionCode) {
                    continue;
                }
                System.out.println("local upgrade app from: " + upgradeRuleHolder.fromVersionCode + " to: " + upgradeRuleHolder.toVersionCode);
                upgradeRuleHolder.upgradeHandler.doLocalUpgrade();
                preVersionCode = upgradeRuleHolder.toVersionCode;
            }
        }


        FileUtils.write(versionCodeFile, String.valueOf(BuildInfo.versionCode), StandardCharsets.UTF_8);
    }

    private static File resolveClassPathDir() {
        URL configURL = Environment.class.getClassLoader().getResource(Environment.APPLICATION_PROPERTIES);
        if (configURL != null && configURL.getProtocol().equals("file")) {
            File classPathDir = new File(configURL.getFile()).getParentFile();
            String absolutePath = classPathDir.getAbsolutePath();
            if (absolutePath.endsWith("target/classes") // with maven
                    || absolutePath.endsWith("build/resources/main") // with gradle
                    || absolutePath.endsWith("conf") // with distribution
            ) {
                return classPathDir;
            }
        }
        throw new IllegalStateException("can not resolve env: " + configURL);
    }

    private static final List<UpgradeRuleHolder> upgradeRuleHolders = new ArrayList<>();

    @SuppressWarnings("all")
    private static void registerUpgradeTask(int fromVersionCode, int toVersionCode, UpgradeHandler upgradeHandler) {
        upgradeRuleHolders.add(new UpgradeRuleHolder(fromVersionCode, toVersionCode, upgradeHandler));
    }

//    static {
//
//        registerUpgradeTask(-1, 1, new SQLExecuteUpgradeHandler("upgrade_v1_v2.sql"));
//    }

    @AllArgsConstructor
    private static class UpgradeRuleHolder {
        private int fromVersionCode;
        private int toVersionCode;
        private UpgradeHandler upgradeHandler;
    }


    public static void registerShutdownHook(Runnable runnable) {
        ShutdownHook.registerShutdownHook(runnable);
    }


    public static int prepareShutdown() {
        return ShutdownHook.prepareShutdown();
    }
}
