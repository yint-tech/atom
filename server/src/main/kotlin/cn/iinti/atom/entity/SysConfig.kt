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

@Schema(name = "SysConfig对象", description = "系统配置")
@TableName("sys_config")
@Accessors(chain = true)
@Data
@EqualsAndHashCode(callSuper = false)
data class SysConfig(
    @Schema(name = "自增主建")
    @TableId(value = "id", type = IdType.AUTO)
    var id: Long? = null,

    @Schema(name = "key")
    var configKey: String? = null,

    @Schema(name = "value")
    var configValue: String? = null,

    @Schema(name = "创建时间")
    var createTime: LocalDateTime? = null,

    @Schema(name = "配置备注")
    var configComment: String? = null
) : Serializable {

    companion object {
        const val ID = "id"
        const val CONFIG_KEY = "config_key"
        const val CONFIG_VALUE = "config_value"
        const val CREATE_TIME = "create_time"
        const val CONFIG_COMMENT = "config_comment"
    }
}