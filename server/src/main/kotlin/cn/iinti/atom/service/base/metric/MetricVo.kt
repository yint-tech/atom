package cn.iinti.atom.service.base.metric

import com.google.common.collect.Lists
import com.google.common.collect.Maps
import io.micrometer.core.instrument.Meter
import io.swagger.v3.oas.annotations.media.Schema
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.BeanUtils
import proguard.annotation.Keep
import java.time.LocalDateTime

@Keep
class MetricVo() {
    @Schema(name = "指标名称")
    var name: String? = null

    @Schema(name = "时间索引")
    var timeKey: String? = null

    @Schema(name = "精度，分为分钟、小时、天三个维度：minutes/hours/days")
    var accuracy: MetricEnums.MetricAccuracy? = null

    @Schema(name = "分量tag")
    var tags: MutableMap<String, String> = Maps.newHashMap()

    @Schema(name = "指标类型：（counter、gauge、timer，请注意暂时只支持这三种指标）")
    var type: Meter.Type? = null

    @Schema(name = "指标值")
    var value: Double? = null

    @Schema(name = "创建时间")
    var createTime: LocalDateTime? = null

    fun toTagId(): String {
        val tagSegments = Lists.newArrayList<String>()
        tags.forEach { (s, s2) -> tagSegments.add("$s##-##$s2") }
        tagSegments.sort()
        return StringUtils.join(tagSegments, ",")
    }

    companion object {
        fun cloneMetricVo(metricVo: MetricVo): MetricVo {
            val ret = MetricVo()
            BeanUtils.copyProperties(ret, metricVo)
            ret.tags = Maps.newHashMap(metricVo.tags)
            return ret
        }
    }
}