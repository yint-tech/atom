package cn.iinti.atom;

import cn.iinti.atom.entity.type.JSONTypeHandler;
import cn.iinti.atom.service.base.BroadcastService;
import cn.iinti.atom.service.base.config.ConfigService;
import cn.iinti.atom.service.base.env.Environment;
import cn.iinti.atom.service.base.safethread.Looper;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import io.micrometer.core.instrument.util.IOUtils;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.type.EnumOrdinalTypeHandler;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import proguard.annotation.Keep;

import javax.sql.DataSource;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@SpringBootApplication
@EnableAspectJAutoProxy
@MapperScan("cn.iinti.atom.mapper")
@EnableScheduling
@Configuration
@Keep
public class AtomMain implements ApplicationListener<WebServerInitializedEvent> {

    @Getter
    private static final Looper shardThread = new Looper("ShardThread").startLoop();


    @Resource
    private DataSource dataSource;

    @Resource
    private ConfigService configService;

    @Scheduled(fixedRate = 10 * 60 * 1000)
    public void reloadConfig() {
        configService.reloadConfig();
    }

    @PostConstruct
    public void init() {
        // 第一步，在涉及到版本升级的场景下，调用版本管理器，进行升级操作，版本升级操作主要是对数据的表结构进行修改
        try {
            Environment.upgradeIfNeed(dataSource);
        } catch (Throwable throwable) {
            System.out.println("upgrade failed,please contact iint business support");
            throwable.printStackTrace(System.err);
            System.exit(1);
        }
        // 配置加载和刷新，需要放到Main上面，这样配置加载将会在所有bean启动前初始化好，
        // 让业务模块在运行的时候就拿到数据库的配置项
        reloadConfig();
        BroadcastService.register(BroadcastService.Topic.CONFIG, this::reloadConfig);
    }

    @Bean
    public OpenAPI springShopOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Atom")
                        .description("atom系统")
                        .version("v" + BuildInfo.versionName)
                        .license(new License().name("Apache 2.0")
                                .url("https://atom.iinti.cn/atom-doc")))
                .externalDocs(new ExternalDocumentation()
                        .description("iinti")
                        .url("https://iinti.cn"));
    }

    @Override
    public void onApplicationEvent(@Nonnull WebServerInitializedEvent event) {
        Environment.setupApp(event);
    }

    @Keep
    public static void main(String[] args) {
        List<String> argList = Lists.newArrayList(args);
        argList.add("--env.versionCode=" + BuildInfo.versionCode);
        argList.add("--env.versionName=" + BuildInfo.versionName);

        // setup log dir
        boolean hasSetupLogDir = argList.stream().anyMatch(s -> s.contains("--LogbackDir"));
        if (!hasSetupLogDir) {
            File logBase = Environment.runtimeClassPathDir;
            if (logBase.getName().equals("conf")) {
                logBase = logBase.getParentFile();
            }
            argList.add("--LogbackDir=" + new File(logBase, "logs").getAbsolutePath());
        }

        //减少对用户的打扰，各组件大概率不会需要配置的参数，直接硬编码到源码中
        springContextParamSetup(argList);

        InputStream stream = AtomMain.class.getClassLoader().getResourceAsStream("addition.txt");
        if (stream != null) {
            List<String> strings = Splitter.on(CharMatcher.breakingWhitespace())
                    .omitEmptyStrings()
                    .trimResults()
                    .splitToList(IOUtils.toString(stream, StandardCharsets.UTF_8));

            for (String str : strings) {
                if (!argList.contains(str)) {
                    argList.add(str);
                }
            }
        }
        try {
            SpringApplication.run(AtomMain.class, argList.toArray(new String[]{}));
        } catch (Throwable throwable) {
            throwable.printStackTrace(System.err);
            // 如果启动失败，必须退出，否则docker的进程守护无法感知到服务启动失败
            System.exit(1);
        }
    }

    private static void springContextParamSetup(List<String> argList) {
        // setup docker for development
        if (Environment.isIdeDevelopment) {
            argList.add("--spring.docker.compose.file=classpath:develop/docker-compose-local.yaml");
            argList.add("--spring.docker.compose.lifecycle-management=start-only");
        }

        // setup metric config
        checkAddPram(argList, "spring.application.name", BuildInfo.appName);
        checkAddPram(argList, "management.endpoints.web.exposure.include", "*");
        checkAddPram(argList, "management.endpoints.web.base-path", BuildInfo.restfulApiPrefix + "/actuator");
        checkAddPram(argList, "management.metrics.tags.application", BuildInfo.appName);

        // setup mbp
        checkAddPram(argList, "mybatis-plus.configuration.default-enum-type-handler", EnumOrdinalTypeHandler.class.getName());
        checkAddPram(argList, "mybatis-plus.type-handlers-package", JSONTypeHandler.class.getPackageName());

        // setup compression
        checkAddPram(argList, "server.compression.enabled", "true");
        checkAddPram(argList, "server.compression.mime-types",
                "application/json,application/xml,text/html,text/xml,text/plain,application/javascript,text/css"
        );
        checkAddPram(argList, "server.compression.min-response-size", "10");
        checkAddPram(argList, "server.compression.excluded-user-agents", "gozilla,traviata");

        // file upload
        checkAddPram(argList, "spring.servlet.multipart.max-file-size", "100MB");
        checkAddPram(argList, "spring.servlet.multipart.max-request-size", "102MB");

        // setup jackson
        checkAddPram(argList, "spring.jackson.date-format", "yyyy-MM-dd HH:mm:ss");
        checkAddPram(argList, "spring.jackson.time-zone", "GMT+8");

        // jdbc driver, we only support mysql
        checkAddPram(argList, "spring.datasource.driver-class-name", "com.mysql.cj.jdbc.Driver");
    }

    private static void checkAddPram(List<String> argList, String key, String value) {
        for (String exist : argList) {
            if (StringUtils.containsIgnoreCase(exist, key)) {
                return;
            }
        }
        argList.add("--" + key + "=" + value);
    }

}
