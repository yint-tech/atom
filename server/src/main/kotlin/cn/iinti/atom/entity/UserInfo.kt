package cn.iinti.atom.entity

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import io.swagger.v3.oas.annotations.media.Schema
import lombok.Data
import lombok.EqualsAndHashCode
import lombok.experimental.Accessors
import java.io.Serial
import java.io.Serializable
import java.time.LocalDateTime

@Schema(name = "UserInfo对象", description = "用户信息")
@TableName("user_info")
@Accessors(chain = true)
@Data
@EqualsAndHashCode(callSuper = false)
data class UserInfo(
    @Schema(name = "自增主建")
    @TableId(value = "id", type = IdType.AUTO)
    var id: Long? = null,

    @Schema(name = "用户名")
    var userName: String? = null,

    @Schema(name = "密码")
    var password: String? = null,

    @Schema(name = "最后登陆时间")
    var lastActive: LocalDateTime? = null,

    @Schema(name = "创建时间")
    var createTime: LocalDateTime? = null,

    @Schema(name = "登录token")
    var loginToken: String? = null,

    @Schema(name = "api 访问token")
    var apiToken: String? = null,

    @Schema(name = "是否是管理员")
    @get:JvmName("getIsAdmin")
    @set:JvmName("setIsAdmin")
    var isAdmin: Boolean? = null,

    @Schema(name = "最后更新时间")
    var updateTime: LocalDateTime? = null,

    @Schema(name = "用户权限")
    var permission: String? = null
) : Serializable {

    companion object {
        const val ID = "id"
        const val USER_NAME = "user_name"
        const val PASSWORD = "password"
        const val LAST_ACTIVE = "last_active"
        const val CREATE_TIME = "create_time"
        const val LOGIN_TOKEN = "login_token"
        const val API_TOKEN = "api_token"
        const val IS_ADMIN = "is_admin"
        const val UPDATE_TIME = "update_time"
        const val PERMISSION = "permission"
    }
}