package cn.iinti.atom.service.base.env

import cn.iinti.atom.BuildInfo
import cn.iinti.atom.entity.CommonRes
import cn.iinti.atom.service.base.config.Configs
import cn.iinti.atom.utils.CommonUtils
import com.alibaba.fastjson.JSONObject
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.BooleanUtils
import org.springframework.boot.web.context.WebServerInitializedEvent
import org.springframework.context.ApplicationContext
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.sql.SQLException
import javax.sql.DataSource

// 手动实现Lombok的Slf4j功能
private val log = org.slf4j.LoggerFactory.getLogger(Environment::class.java)

class Environment {
    companion object {
        const val APPLICATION_PROPERTIES = "application.properties"

        @JvmStatic
        var tomcatPort: Int = 0
            private set

        @JvmField
        val isLocalDebug: Boolean = BooleanUtils.toBoolean(Configs.getConfig("env.localDebug", "false"))

        @JvmField
        val isDemoSite: Boolean = BooleanUtils.toBoolean(Configs.getConfig("env.demoSite", "false"))

        @JvmField
        val runtimeClassPathDir: File = resolveClassPathDir()

        @JvmField
        val isIdeDevelopment: Boolean = !runtimeClassPathDir.name.equals("conf")

        @JvmField
        val storageRoot: File = CommonUtils.forceMkdir(resolveStorageRoot())

        @JvmStatic
        fun buildInfo(): CommonRes<JSONObject> {
            return CommonRes.success(JSONObject()
                .fluentPut("buildInfo",
                    JSONObject()
                        .fluentPut("versionCode", BuildInfo.versionCode)
                        .fluentPut("versionName", BuildInfo.versionName)
                        .fluentPut("buildTime", BuildInfo.buildTime)
                        .fluentPut("buildUser", BuildInfo.buildUser)
                        .fluentPut("gitId", GitProperties.IT.GIT_ID.value)
                ).fluentPut("env",
                    JSONObject()
                        .fluentPut("demoSite", isDemoSite)
                        .fluentPut("debug", isLocalDebug)
                )
            )
        }

        // 手动实现@Getter功能
        @Volatile
        private var _app: ApplicationContext? = null
        val app: ApplicationContext
            get() {
                val currentApp = _app
                if (currentApp == null) {
                    throw IllegalStateException("ApplicationContext not initialized")
                }
                return currentApp
            }

        @JvmStatic
        fun setupApp(event: WebServerInitializedEvent) {
            _app = event.applicationContext
            tomcatPort = event.webServer.port
        }

        @Throws(SQLException::class)
        @JvmStatic
        fun upgradeIfNeed(dataSource: DataSource) {
            upgradeRuleHolders.sortBy { it.fromVersionCode }
            doDbUpGradeTask(dataSource)

            if (isIdeDevelopment) {
                // 本地代码执行模式，认为一定是最新版本，不需要执行升级代码
                return
            }
            doLocalUpGradeTask(File(runtimeClassPathDir, "versionCode.txt"))
            println("app: ${BuildInfo.appName} version:(${BuildInfo.versionCode}:${BuildInfo.versionName}) buildTime:${BuildInfo.buildTime}")
        }

        private const val DB_VERSION_SQL = "select config_value from sys_config where config_key='_atom_framework_version' and config_comment='_atom_framework'"
        private const val UPDATE_DB_VERSION_SQL = "insert into sys_config (`config_comment`,`config_key`,`config_value`) values ('_atom_framework','_atom_framework_version','${BuildInfo.versionCode}') on duplicate key update `config_value`='${BuildInfo.versionCode}'"

        @Throws(SQLException::class)
        private fun doDbUpGradeTask(dataSource: DataSource) {
            dataSource.connection.use { conn ->
                // 获取之前的版本
                conn.createStatement().use { statement ->
                    statement.executeQuery(DB_VERSION_SQL).use { resultSet ->
                        if (resultSet.next()) {
                            var preVersionCode = Integer.parseInt(resultSet.getString(1))

                            for (upgradeRuleHolder in upgradeRuleHolders) {
                                if (upgradeRuleHolder.fromVersionCode < preVersionCode) {
                                    continue
                                }
                                println("db upgrade app from: ${upgradeRuleHolder.fromVersionCode} to: ${upgradeRuleHolder.toVersionCode}")
                                upgradeRuleHolder.upgradeHandler.doDbUpgrade(dataSource)
                                preVersionCode = upgradeRuleHolder.toVersionCode
                            }
                        }
                    }
                }

                // 更新当前版本
                conn.createStatement().use { statement ->
                    statement.execute(UPDATE_DB_VERSION_SQL)
                }
            }
        }

        @Throws(IOException::class)
        private fun doLocalUpGradeTask(versionCodeFile: File) {
            if (versionCodeFile.exists()) {
                var preVersionCode = Integer.parseInt(FileUtils.readFileToString(versionCodeFile, StandardCharsets.UTF_8))

                for (upgradeRuleHolder in upgradeRuleHolders) {
                    if (upgradeRuleHolder.fromVersionCode < preVersionCode) {
                        continue
                    }
                    println("local upgrade app from: ${upgradeRuleHolder.fromVersionCode} to: ${upgradeRuleHolder.toVersionCode}")
                    upgradeRuleHolder.upgradeHandler.doLocalUpgrade()
                    preVersionCode = upgradeRuleHolder.toVersionCode
                }
            }

            FileUtils.write(versionCodeFile, BuildInfo.versionCode.toString(), StandardCharsets.UTF_8)
        }

        private fun resolveClassPathDir(): File {
            val configURL = Environment::class.java.classLoader.getResource(Environment.APPLICATION_PROPERTIES)
            if (configURL != null && configURL.protocol.equals("file")) {
                val classPathDir = File(configURL.file).parentFile
                val absolutePath = classPathDir.absolutePath
                if (absolutePath.endsWith("target/classes") || // Maven
                    absolutePath.endsWith("build/resources/main") || // Gradle
                    absolutePath.endsWith("conf") || // 分发版本
                    absolutePath.endsWith("build\\resources\\main") // Windows上的Gradle
                ) {
                    return classPathDir
                }
            }
            throw IllegalStateException("can not resolve env: $configURL")
        }

        private fun resolveStorageRoot(): File {
            return if (isIdeDevelopment) {
                File(FileUtils.getUserDirectory(), BuildInfo.appName)
            } else {
                File(runtimeClassPathDir.parent, "data")
            }
        }

        val upgradeRuleHolders: MutableList<UpgradeRuleHolder> = ArrayList()

        @Suppress("all")
        private fun registerUpgradeTask(fromVersionCode: Int, toVersionCode: Int, upgradeHandler: UpgradeHandler) {
            (upgradeRuleHolders as ArrayList<UpgradeRuleHolder>).add(UpgradeRuleHolder(fromVersionCode, toVersionCode, upgradeHandler))
        }

        @JvmStatic
        fun registerShutdownHook(runnable: Runnable) {
            ShutdownHook.registerShutdownHook(runnable)
        }

        @JvmStatic
        fun prepareShutdown(): Int {
            return ShutdownHook.prepareShutdown()
        }
    }

    class UpgradeRuleHolder(val fromVersionCode: Int, val toVersionCode: Int, val upgradeHandler: UpgradeHandler)
}