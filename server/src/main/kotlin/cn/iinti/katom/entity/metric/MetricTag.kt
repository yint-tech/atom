package cn.iinti.katom.entity.metric

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import io.swagger.v3.oas.annotations.media.Schema
import lombok.Data
import lombok.EqualsAndHashCode
import lombok.experimental.Accessors
import java.io.Serializable

@Schema(name = "MetricTag对象", description = "指标tag定义")
@TableName("metric_tag")
@Accessors(chain = true)
@Data
@EqualsAndHashCode(callSuper = false)
data class MetricTag(
    @Schema(name = "自增主建")
    @TableId(value = "id", type = IdType.AUTO)
    var id: Long? = null,

    @Schema(name = "指标名称")
    var name: String? = null,

    @Schema(name = "指标tag")
    var tag1Name: String? = null,

    @Schema(name = "指标tag")
    var tag2Name: String? = null,

    @Schema(name = "指标tag")
    var tag3Name: String? = null,

    @Schema(name = "指标tag")
    var tag4Name: String? = null,

    @Schema(name = "指标tag")
    var tag5Name: String? = null
) : Serializable {

    companion object {
        const val ID = "id"
        const val NAME = "name"
        const val TAG1_NAME = "tag1_name"
        const val TAG2_NAME = "tag2_name"
        const val TAG3_NAME = "tag3_name"
        const val TAG4_NAME = "tag4_name"
        const val TAG5_NAME = "tag5_name"
    }
}