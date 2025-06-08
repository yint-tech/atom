import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

var versionCode by extra(1)
var versionName by extra("1.0.0-SNAPSHOT")
var applicationId by extra("cn.iinti.katom")
var docPath by extra("katom-doc")
var userLoginTokenKey by extra("Atom-Token")
var restfulApiPrefix by extra("/katom-api")
var appName by extra("katom")
var enableAmsNotice by extra(false)

var buildTime: String by extra(
    LocalDateTime.now().format(
        DateTimeFormatter.ofPattern(
            "yyyy-MM-dd_HH:mm:ss"
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

// 因体产品开关
var yIntProject by extra(false)