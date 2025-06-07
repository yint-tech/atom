package cn.iinti.atom.service.base.metric.mql.func

import cn.iinti.atom.service.base.metric.MetricEnums
import cn.iinti.atom.service.base.metric.MetricVo
import cn.iinti.atom.service.base.metric.MetricVo.Companion.cloneMetricVo
import cn.iinti.atom.service.base.metric.mql.func.MQLFunction.MQL_FUNC
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.google.common.collect.Sets
import io.micrometer.core.instrument.Meter
import org.apache.commons.lang3.StringUtils
import java.lang.Double.sum

import kotlin.math.max


/**
 * a aggregate function
 */
@MQL_FUNC("aggregate")
class FuncAggregate(params: List<String>) : XofVarFunc(params) {
    private val aggregateFields: MutableSet<String> = Sets.newHashSet()

    init {
        check(params.size >= 2) { "a filter function must have more than 2 params" }
        for (i in 1..<params.size) {
            aggregateFields.add(params[i])
        }
    }


    override fun apply(metricVos: List<MetricVo>): MutableList<MetricVo> {
        val group: MutableMap<String, MutableList<MetricVo>> = Maps.newHashMap()

        metricVos.forEach { metricVo: MetricVo ->
            val tags: Map<String, String> = metricVo.tags
            val keySegment: MutableList<String> = Lists.newArrayList()
            tags.forEach { (tag: String, value: String) ->
                if (!aggregateFields.contains(tag)) {
                    keySegment.add("$tag#-#$value")
                }
            }
            keySegment.sort()

            group.computeIfAbsent(StringUtils.join(keySegment, ",")) { _: String? -> Lists.newArrayList() }
                .add(metricVo)
        }
        return group.values
            .map { doAggregate(it) }
            .toMutableList()
    }

    private fun doAggregate(metricVos: List<MetricVo>): MetricVo {
        val metricVo = cloneMetricVo(metricVos[0])
        val tags = metricVo.tags
        //this field will be removed  after filter
        aggregateFields.forEach { o: String -> tags.remove(o) }

        if (metricVos.size == 1) {
            return metricVo
        }
        when (metricVo.type) {
            Meter.Type.COUNTER, Meter.Type.GAUGE ->                 // sum
                metricVo.value = metricVos.sumOf { it.value!! }

            Meter.Type.TIMER -> {
                val timerType = metricVo.tags[MetricEnums.TimeSubType.TIMER_TYPE]
                if (StringUtils.isBlank(timerType) || timerType == MetricEnums.TimeSubType.MAX.metricKey) {
                    // this is aggregated time-max
                    metricVo.value = metricVos.maxOf { it.value!! }
                } else if (timerType == MetricEnums.TimeSubType.TIME.metricKey
                    || timerType == MetricEnums.TimeSubType.COUNT.metricKey
                ) {
                    metricVo.value = metricVos.sumOf { it.value!! }
                }
            }

            else -> throw IllegalStateException("unknow metric type: ${metricVo.type}")
        }
        return metricVo
    }
}
