package cn.iinti.atom.service.base.metric

import cn.iinti.atom.service.base.metric.mql.Context.MQLVar
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.google.common.collect.Sets
import lombok.Data
import org.apache.commons.lang3.StringUtils
import proguard.annotation.Keep
import java.util.*
import java.util.function.Consumer


/**
 * show mql query data as EChart Option
 */
@Data
@Keep
class EChart4MQL {
    /**
     * 共有多少条线
     */
    private val legends: MutableList<String> = Lists.newArrayList()

    /**
     * x坐标，每当有一个时间点（time），则存在一个值，里面是时间点集合
     */
    private val xAxis: MutableList<String> = Lists.newArrayList()


    /**
     * 具体的数据，每个子List代表一条线，List的顺序和数量和legends对其
     */
    private val series: MutableList<Serial?> = Lists.newArrayList()

    @Data
    @Keep
    class Serial {
        internal var name: String? = null
        private val type = "line"
        val data: MutableList<Double> = Lists.newArrayList()
    }

    companion object {
        @JvmStatic
        fun legendId(varName: String, metricVo: MetricVo, singleVar: Boolean): String {
            val tagDisplayIds: MutableList<String> = Lists.newArrayList()
            val tags = metricVo.tags
            if (tags.size == 1) {
                tagDisplayIds.add(tags.values.iterator().next())
            } else {
                tags.forEach { (tag: String?, value: String?) -> tagDisplayIds.add("$tag#$value") }
                Collections.sort(tagDisplayIds)
            }
            if (!singleVar) {
                tagDisplayIds.add(0, varName)
            }
            if (tagDisplayIds.isEmpty()) {
                return varName
            }
            return StringUtils.join(tagDisplayIds, "-")
        }

        fun fromMQLResult(exportData: Map<String?, MQLVar?>): EChart4MQL {
            val eChart4MQL = EChart4MQL()

            val legendSet: MutableSet<String> = Sets.newTreeSet()

            // 遍历所有的数据，计算有多少条折线，这是因为每个指标都可能有tag（即子维度，那么线条渲染需要拆分为多个子维度）
            //------ time------- metric----- value
            val metricGroupByTime = Maps.newTreeMap<String, MutableMap<String, List<MetricVo>>>()
            val singleVar = exportData.size == 1
            exportData.forEach { (varName: String?, mqlVar: MQLVar?) ->
                mqlVar!!.data.forEach { (timeStr: String?, metricVos: MutableList<MetricVo>) ->
                    val mapTime = metricGroupByTime.computeIfAbsent(timeStr) { s: String? -> Maps.newHashMap() }
                    mapTime[varName!!] = metricVos
                    if (metricVos.isEmpty()) {
                        // 指标为空，代表整个指标都被过滤了,此时保护指标，后续流程会填充0值
                        legendSet.add(varName)
                    } else {
                        metricVos.forEach(Consumer { metricVo: MetricVo ->  // many sub tag data for an xAxis
                            legendSet.add(legendId(varName, metricVo, singleVar))
                        })
                    }
                }
            }

            // 数据容器初始化
            val legends = eChart4MQL.legends
            legends.addAll(legendSet)
            val series = eChart4MQL.series
            val serialRef: MutableMap<String, Serial> = Maps.newHashMap()
            legends.forEach(Consumer<String> { legend: String ->
                val serial = Serial()
                serial.name = legend
                series.add(serial)
                serialRef[legend] = serial
            })

            // 填充数据
            val xAxis = eChart4MQL.xAxis
            metricGroupByTime.forEach { (time: String, timeData: MutableMap<String, List<MetricVo>>) ->
                // 一个X坐标
                xAxis.add(time)
                // 当前X坐标下，对应的Y值
                val points: MutableSet<Serial?> = Sets.newHashSet(series)

                timeData.forEach { (varName: String, metricVos: List<MetricVo>) ->
                    metricVos.forEach(
                        Consumer { metricVo: MetricVo ->
                            val legend = legendId(varName, metricVo, singleVar)
                            val line = serialRef[legend]
                            points.remove(line)
                            line!!.data.add(metricVo.value!!)
                        })
                }

                // 当前X下，没有对应Y值，则设置为0
                points.forEach(Consumer { doubles: Serial? -> doubles!!.data.add(0.0) })
            }
            return eChart4MQL
        }
    }
}
