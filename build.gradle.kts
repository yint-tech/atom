import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

var versionCode by extra(1)
var versionName by extra("1.0.0-SNAPSHOT")
var applicationId by extra("cn.iinti.atom")
var docPath by extra("atom-doc")
var userLoginTokenKey by extra("AtomToken")
var restfulApiPrefix by extra("/atom-api")
var appName by extra("atom")

var buildTime: String by extra(
    LocalDateTime.now().format(
        DateTimeFormatter.ofPattern(
            "yyyy-MM-dd_HH:mm:ss",
            java.util.Locale.CHINA
        )
    )
)

var buildUser: String by extra {
    var user = System.getenv("USER")
    if (user == null || user.isEmpty()) {
        user = System.getenv("USERNAME")
    }
    user
}

// 前端工具链相关
val yarnVersionStr by extra("4.1.1")
val nodeVersionStr by extra("20.10.0")
var nodeDistMirror by extra("https://mirrors.ustc.edu.cn/node")

// 发布相关，每个新项目核心需要关注这里的配置
var deployServerList by extra(listOf("atom.iinti.cn"))
var deployRemoteUser by extra("root")
var deployPath by extra("/opt/atom/")
var deployFileServer by extra("oss.iinti.cn")
var deployFileAssetPath by extra("/root/local-deplpy/gohttpserver/data/atom/")
var deployDockerRegistry by extra("registry.cn-beijing.aliyuncs.com/iinti/common")

// 代码保护相关,请注意本模块仅限因体产品线支持，开源生态没有代码保护和授权管理模块
var protectionEnable by extra(false)
var protectionMainJar by extra("atom-server")
var protectionSlaveJar by extra(arrayOf("mybatis-plus-extension"))
