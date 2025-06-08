package cn.iinti.katom

import cn.iinti.atom.BuildInfo
import cn.iinti.katom.entity.type.JSONTypeHandler
import cn.iinti.katom.base.BroadcastService
import cn.iinti.katom.base.config.ConfigService
import cn.iinti.katom.base.env.Environment
import cn.iinti.katom.base.safethread.Looper
import com.google.common.base.CharMatcher
import com.google.common.base.Splitter
import com.google.common.collect.Lists
import io.micrometer.core.instrument.util.IOUtils
import jakarta.annotation.Nonnull
import jakarta.annotation.PostConstruct
import jakarta.annotation.Resource
import org.apache.commons.lang3.StringUtils
import org.apache.ibatis.type.EnumOrdinalTypeHandler
import org.mybatis.spring.annotation.MapperScan
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.web.context.WebServerInitializedEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import proguard.annotation.Keep
import java.io.File
import java.nio.charset.StandardCharsets
import javax.sql.DataSource
import kotlin.system.exitProcess

@SpringBootApplication
@EnableAspectJAutoProxy
@MapperScan("cn.iinti.katom.mapper")
@EnableScheduling
@Configuration
@Keep
class AtomMain : ApplicationListener<WebServerInitializedEvent> {

    companion object {

        val shardThread = Looper("ShardThread").startLoop()

        @JvmStatic
        fun main(args: Array<String>) {
            val argList = Lists.newArrayList(*args)
            argList.add("--env.versionCode=" + BuildInfo.versionCode)
            argList.add("--env.versionName=" + BuildInfo.versionName)

            // setup log dir
            var hasSetupLogDir = false
            for (s in argList) {
                if (s.contains("--LogbackDir")) {
                    hasSetupLogDir = true
                    break
                }
            }
            if (!hasSetupLogDir) {
                var logBase = Environment.runtimeClassPathDir
                if (logBase.name == "conf") {
                    logBase = logBase.parentFile!!
                } else if (logBase.toString().endsWith("build/resources/main")) {
                    logBase = logBase.parentFile!!.parentFile!!
                }
                argList.add("--LogbackDir=" + File(logBase, "logs").absolutePath)
            }

            //减少对用户的打扰，各组件大概率不会需要配置的参数，直接硬编码到源码中
            springContextParamSetup(argList)

            val stream = AtomMain::class.java.classLoader.getResourceAsStream("addition.txt")
            if (stream != null) {
                val strings = Splitter.on(CharMatcher.breakingWhitespace())
                    .omitEmptyStrings()
                    .trimResults()
                    .splitToList(IOUtils.toString(stream, StandardCharsets.UTF_8))

                for (str in strings) {
                    if (!argList.contains(str)) {
                        argList.add(str)
                    }
                }
            }
            runApp(argList, { throwable ->
                if (Environment.isIdeDevelopment && StringUtils.contains(
                        throwable.message,
                        "Docker is not running"
                    )
                ) {
                    // 有一些时候用户在本地开发的时候,不想依赖docker，那么这个时候尝试禁用docker，直接使用本机配置尝试启动服务
                    argList.add("--spring.docker.compose.enabled=false")
                    throwable.printStackTrace(System.err)
                    runApp(argList, defaultErrorHandle)
                    return@runApp
                }
                defaultErrorHandle.invoke(throwable)
            })
        }

        private fun runApp(argList: MutableList<String>, errorHandle: (Throwable) -> Unit) {
            try {
                SpringApplication.run(AtomMain::class.java, *argList.toTypedArray())
            } catch (throwable: Throwable) {
                errorHandle(throwable)
            }
        }

        private val defaultErrorHandle: (Throwable) -> Unit = { throwable: Throwable ->
            throwable.printStackTrace(System.err)
            // 如果启动失败，必须退出，否则docker的进程守护无法感知到服务启动失败
            exitProcess(1)
        }

        private fun springContextParamSetup(argList: MutableList<String>) {
            argList.add("--spring.main.allow-circular-references=true")
            // setup docker for development
            if (Environment.isIdeDevelopment) {
                argList.add("--spring.docker.compose.file=classpath:develop-atom/docker-compose-local.yaml")
                argList.add("--spring.docker.compose.lifecycle-management=start-only")
            }

            // setup metric config
            checkAddPram(argList, "spring.application.name", BuildInfo.appName)
            checkAddPram(argList, "management.endpoints.web.exposure.include", "*")
            checkAddPram(argList, "management.endpoints.web.base-path", BuildInfo.restfulApiPrefix + "/actuator")
            checkAddPram(argList, "management.metrics.tags.application", BuildInfo.appName)

            // setup mbp
            checkAddPram(
                argList,
                "mybatis-plus.configuration.default-enum-type-handler",
                EnumOrdinalTypeHandler::class.java.name
            )
            checkAddPram(argList, "mybatis-plus.type-handlers-package", JSONTypeHandler::class.java.packageName)

            // setup compression
            checkAddPram(argList, "server.compression.enabled", "true")
            checkAddPram(
                argList, "server.compression.mime-types",
                "application/json,application/xml,text/html,text/xml,text/plain,application/javascript,text/css"
            )
            checkAddPram(argList, "server.compression.min-response-size", "10")
            checkAddPram(argList, "server.compression.excluded-user-agents", "gozilla,traviata")

            // file upload
            checkAddPram(argList, "spring.servlet.multipart.max-file-size", "100MB")
            checkAddPram(argList, "spring.servlet.multipart.max-request-size", "102MB")

            // setup jackson
            checkAddPram(argList, "spring.jackson.date-format", "yyyy-MM-dd HH:mm:ss")
            checkAddPram(argList, "spring.jackson.time-zone", "GMT+8")

            // jdbc driver, we only support mysql
            checkAddPram(argList, "spring.datasource.driver-class-name", "com.mysql.cj.jdbc.Driver")
        }

        private fun checkAddPram(argList: MutableList<String>, key: String, value: String) {
            for (exist in argList) {
                if (StringUtils.containsIgnoreCase(exist, key)) {
                    return
                }
            }
            argList.add("--$key=$value")
        }
    }

    @Resource
    private lateinit var dataSource: DataSource

    @Resource
    private lateinit var configService: ConfigService

    @Scheduled(fixedRate = 10 * 60 * 1000)
    fun reloadConfig() {
        configService.reloadConfig()
    }

    @PostConstruct
    fun init() {
        // 第一步，在涉及到版本升级的场景下，调用版本管理器，进行升级操作，版本升级操作主要是对数据的表结构进行修改
        try {
            Environment.upgradeIfNeed(dataSource)
        } catch (throwable: Throwable) {
            println("upgrade failed,please contact iinti business support")
            throwable.printStackTrace(System.err)
            exitProcess(1)
        }
        // 配置加载和刷新，需要放到Main上面，这样配置加载将会在所有bean启动前初始化好，
        // 让业务模块在运行的时候就拿到数据库的配置项
        reloadConfig()
        BroadcastService.register(BroadcastService.Topic.CONFIG) {
            reloadConfig()
        }
    }

    override fun onApplicationEvent(@Nonnull event: WebServerInitializedEvent) {
        Environment.setupApp(event)
    }
}