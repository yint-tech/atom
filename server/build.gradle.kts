import org.gradle.api.internal.plugins.DefaultJavaAppStartScriptGenerationDetails

plugins {
    java
    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.4"
    id("org.springdoc.openapi-gradle-plugin") version "1.8.0"
    id("com.gorylenko.gradle-git-properties") version "2.4.2"
    application
}

gitProperties {
    failOnNoGitDirectory = false
    extProperty = "gitProps"
}

val applicationId: String by rootProject.extra
var versionCode: Int by rootProject.extra
var versionName: String by rootProject.extra
var buildTime: String by rootProject.extra
var buildUser: String by rootProject.extra
var docPath: String by rootProject.extra
var userLoginTokenKey: String by rootProject.extra
var restfulApiPrefix: String by rootProject.extra
var appName: String by rootProject.extra


group = applicationId
version = versionName

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web") {
        exclude(group = "org.apache.logging.log4j", module = "log4j-to-slf4j")
    }
    implementation("jakarta.servlet:jakarta.servlet-api:5.0.0")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation("com.baomidou:mybatis-plus-spring-boot3-starter:3.5.6")
    testImplementation("com.baomidou:mybatis-plus-generator:3.5.3.2")

    compileOnly("org.projectlombok:lombok")
    testImplementation("org.projectlombok:lombok")
    compileOnly("net.sf.proguard:proguard-annotations:6.2.2")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.apache.velocity:velocity-engine-core:2.3")
    implementation("mysql:mysql-connector-java:8.0.28")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")

    implementation("ch.qos.logback:logback-classic:1.4.12")
    implementation("org.slf4j:jcl-over-slf4j:1.7.30")
    implementation("org.slf4j:log4j-over-slf4j:1.7.30")
    implementation("com.google.guava:guava:31.1-jre")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("commons-io:commons-io:2.10.0")
    implementation("com.alibaba:fastjson:1.2.79")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

}

tasks.test {
    useJUnitPlatform()
}


application {
    mainClass = "cn.iinti.atom.AtomMain"
    applicationName = "Atom"
    applicationDefaultJvmArgs = listOf(
        "-Dfile.encoding=utf-8", "-Duser.timezone=GMT+08", "-XX:-OmitStackTraceInFastThrow"
    )
    applicationDistribution.from("${projectDir}/src/main/resources") {
        include("application.properties")
        into("conf/")
    }
    applicationDistribution.from("${projectDir}/src/main/resources/develop") {
        include("ddl.sql")
        into("conf/")
    }
    applicationDistribution.from("${projectDir}/assets") {
        include("startup.sh")
        into("bin/")
    }


    applicationDistribution.from("${projectDir}/frontend/build") {
        into("conf/static/")
    }
    applicationDistribution.from("${projectDir}/doc/src/.vuepress/dist") {
        into("conf/static/atom-doc")
    }
}

tasks.getByPath("startScripts").doFirst {
    (this as CreateStartScripts).let {
        fun wrapScriptGenerator(delegate: ScriptGenerator): ScriptGenerator {
            return ScriptGenerator { details, destination ->
                // 增加一个conf的目录，作为最终目标的classPath，在最终发布的时候，我们需要植入静态资源
                (details as DefaultJavaAppStartScriptGenerationDetails).classpath
                    .add(0, "conf")
                delegate.generateScript(details, destination)
            }
        }
        unixStartScriptGenerator = wrapScriptGenerator(unixStartScriptGenerator)
        windowsStartScriptGenerator = wrapScriptGenerator(windowsStartScriptGenerator)
    }
}

sourceSets {
    main {
        java {
            srcDir("build/generated/java")
        }
    }
}



tasks.register("generateJavaCode") {
    doLast {
        val generatedDir = file("build/generated/java").resolve(
            applicationId.replace('.', '/')
        )
        generatedDir.mkdirs()
        val className = "BuildInfo"
        val sourceFile = File(generatedDir, "$className.java")


        //public static final String gitId ="${rootProject.property("git.commit.id")}";
        sourceFile.writeText(
            """
            package ${applicationId};

            public class $className {
                    public static final int versionCode = ${versionCode};
                    public static final String versionName = "$versionName";
                    public static final String buildTime ="$buildTime";
                    public static final String buildUser ="$buildUser";
                    public static final String docPath ="$docPath";
                    public static final String userLoginTokenKey ="$userLoginTokenKey";
                    public static final String restfulApiPrefix ="$restfulApiPrefix";
                    public static final String appName ="$appName";
            }
        """.trimIndent()
        )
    }
}


tasks.named("compileJava") {
    dependsOn("generateJavaCode")
}

afterEvaluate {
    tasks.startScripts {
        dependsOn(":frontend:yarnBuild")
        dependsOn(":doc:yarnBuild")
    }
}


