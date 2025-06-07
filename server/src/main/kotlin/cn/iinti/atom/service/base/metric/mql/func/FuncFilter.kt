package cn.iinti.atom.service.base.metric.mql.func

import cn.iinti.atom.service.base.metric.MetricEnums
import cn.iinti.atom.service.base.metric.MetricVo
import cn.iinti.atom.service.base.metric.mql.func.MQLFunction.MQL_FUNC
import com.google.common.collect.Maps
import io.micrometer.core.instrument.Meter
import org.apache.commons.lang3.StringUtils
import java.util.function.Consumer
import java.util.stream.Collectors


/**
 * taskEnd[status=true]
 * filter(taskEnd,"status=true")
 */
@MQL_FUNC("filter")
class FuncFilter(params: List<String>) : XofVarFunc(params) {
    private val filters: MutableMap<String, String> = Maps.newHashMap()


    init {
        val size = params.size
        check((size and 0x01) == 1) { "filter function must be" }
        var i = 1
        while (i < params.size) {
            filters[params[i]] = params[i + 1]
            i += 2
        }
    }


    override fun apply(metricVos: List<MetricVo>): MutableList<MetricVo> {
        return metricVos.stream()
            .filter { metricVo: MetricVo? ->
                val tags: Map<String, String> = metricVo!!.tags
                filters.entries
                    .stream()
                    .allMatch { entry: Map.Entry<String, String> ->
                        StringUtils.equals(
                            tags[entry.key], entry.value
                        )
                    }
            }.peek { metricVo: MetricVo? ->
                val metricTimeType = metricVo!!.tags[MetricEnums.TimeSubType.TIMER_TYPE]
                switchMetricType4TimeFilter(metricVo, metricTimeType)

                val tags = metricVo.tags
                //this field will be removed  after filter
                filters.keys.forEach(Consumer { o: String -> tags.remove(o) })
            }
            .collect(Collectors.toList())
    }

    companion object {
        fun switchMetricType4TimeFilter(metricVo: MetricVo, metricTimeType: String?) {
            if (StringUtils.isBlank(metricTimeType)) {
                return
            }
            if (metricVo.type != Meter.Type.TIMER) {
                return
            }

            // if filter time field, switch metric type
            if (MetricEnums.TimeSubType.TIME.metricKey == metricTimeType ||
                MetricEnums.TimeSubType.COUNT.metricKey == metricTimeType
            ) {
                metricVo.type = Meter.Type.COUNTER
            }
        }
    }
}
