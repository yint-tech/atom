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

@Schema(name = "ServerNode对象", description = "服务器节点，多台服务器组成代理集群")
@TableName("server_node")
@Accessors(chain = true)
@Data
@EqualsAndHashCode(callSuper = false)
data class ServerNode(
    @Schema(name = "自增主建")
    @TableId(value = "id", type = IdType.AUTO)
    var id: Long? = null,

    @Schema(name = "服务器id，唯一标记服务器")
    var serverId: String? = null,

    @Schema(name = "最后心跳时间")
    var lastActiveTime: LocalDateTime? = null,

    @Schema(name = "创建时间")
    var createTime: LocalDateTime? = null,

    @Schema(name = "工作IP")
    var ip: String? = null,

    @Schema(name = "springboot 服务开启端口")
    var port: Int? = null,

    @Schema(name = "服务器是否启用")
    var enable: Boolean? = null,

    @Schema(name = "本地ip")
    var localIp: String? = null,

    @Schema(name = "工作ip")
    var outIp: String? = null
) : Serializable {

    companion object {
        const val ID = "id"
        const val SERVER_ID = "server_id"
        const val LAST_ACTIVE_TIME = "last_active_time"
        const val CREATE_TIME = "create_time"
        const val IP = "ip"
        const val PORT = "port"
        const val ENABLE = "enable"
        const val LOCAL_IP = "local_ip"
        const val OUT_IP = "out_ip"
    }
}