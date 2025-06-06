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

@Schema(name = "SysLog对象", description = "")
@TableName("sys_log")
@Accessors(chain = true)
@Data
@EqualsAndHashCode(callSuper = false)
data class SysLog(
    @Schema(name = "自增主键")
    @TableId(value = "id", type = IdType.AUTO)
    var id: Long? = null,

    @Schema(name = "操作用户名")
    var username: String? = null,

    @Schema(name = "操作")
    var operation: String? = null,

    @Schema(name = " 操作参数")
    var params: String? = null,

    @Schema(name = "操作的方法名")
    var methodName: String? = null,

    @Schema(name = "创建时间")
    var createTime: LocalDateTime? = null
) : Serializable {

    companion object {
        const val ID = "id"
        const val USERNAME = "username"
        const val OPERATION = "operation"
        const val PARAMS = "params"
        const val METHOD_NAME = "method_name"
        const val CREATE_TIME = "create_time"
    }
}