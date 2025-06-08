package cn.iinti.katom.entity.metric

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableId
import io.micrometer.core.instrument.Meter
import io.swagger.v3.oas.annotations.media.Schema
import lombok.Data
import lombok.EqualsAndHashCode
import lombok.experimental.Accessors
import java.io.Serializable
import java.time.LocalDateTime


@Schema(name = "Metric对象", description = "监控指标")
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
open class Metric(
    @Schema(name = "自增主建")
    @TableId(value = "id", type = IdType.AUTO)
    var id: Long? = null,

    @Schema(name = "指标名称")
    var name: String? = null,

    @Schema(name = "时间索引")
    var timeKey: String? = null,

    @Schema(name = "对于tag字段自然顺序拼接求md5")
    var tagsMd5: String? = null,

    @Schema(name = "指标tag")
    var tag1: String? = null,

    @Schema(name = "指标tag")
    var tag2: String? = null,

    @Schema(name = "指标tag")
    var tag3: String? = null,

    @Schema(name = "指标tag")
    var tag4: String? = null,

    @Schema(name = "指标tag")
    var tag5: String? = null,

    @Schema(name = "指标类型：（counter、gauge、timer，请注意暂时只支持这三种指标）")
    var type: Meter.Type? = null,

    @Schema(name = "指标值")
    var value: Double? = null,

    @Schema(name = "创建时间")
    var createTime: LocalDateTime? = null
) : Serializable {

    companion object {
        const val ID = "id"
        const val NAME = "name"
        const val TIME_KEY = "time_key"
        const val TAGS_MD5 = "tags_md5"
        const val TAG1 = "tag1"
        const val TAG2 = "tag2"
        const val TAG3 = "tag3"
        const val TAG4 = "tag4"
        const val TAG5 = "tag5"
        const val TYPE = "type"
        const val VALUE = "value"
        const val CREATE_TIME = "create_time"
    }
}