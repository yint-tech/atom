package cn.iinti.atom.service.base.alert

import cn.iinti.atom.service.base.alert.events.MetricMonitorConfig.MetricData
import cn.iinti.atom.service.base.metric.MetricEnums
import cn.iinti.atom.service.base.metric.MetricEnums.MetricAccuracy
import cn.iinti.atom.service.base.metric.MetricService
import cn.iinti.atom.service.base.metric.MetricVo
import cn.iinti.atom.service.base.metric.MetricVo.Companion.cloneMetricVo
import cn.iinti.atom.service.base.metric.mql.Context.MQLVar
import cn.iinti.atom.service.base.metric.mql.MQL
import groovy.lang.Closure
import io.micrometer.core.instrument.Meter
import org.apache.commons.lang3.StringUtils
import org.springframework.scheduling.support.CronExpression
import java.time.LocalDateTime
import java.util.*
import java.util.function.Function
import java.util.function.Predicate
import java.util.stream.Collectors
import java.util.stream.Stream


class MetricMonitorHandle {
    private var mql: MQL? = null
    private var cron: CronExpression? = null
    private var start: LocalDateTime? = null
    private var end: LocalDateTime? = null
    private var accuracy: MetricAccuracy? = null
    private var callback: Closure<*>? = null

    private var lastRun: LocalDateTime? = null

    fun evaluate(metricService: MetricService, force: Boolean) {
        if (!force && !isCronRunTime) {
            return
        }
        if (accuracy == null) {
            accuracy = MetricAccuracy.HOURS
        }
        if (start != null) {
            trimAccuracy()
        }
        lastRun = LocalDateTime.now()
        val metricData = mql!!.run(accuracy!!, metricService)
        val map = filterAndMerge(metricData)

        val delegate = MetricData(map)
        callback!!.rehydrate(delegate, callback!!.owner, callback!!.thisObject)
            .call()
    }

    private fun trimAccuracy() {
        if (accuracy == MetricAccuracy.DAYS) {
            return
        }
        if (start!!.isBefore(LocalDateTime.now().minusDays(30))) {
            accuracy = MetricAccuracy.DAYS
            return
        }
        if (accuracy == MetricAccuracy.HOURS) {
            return
        }
        if (start!!.isBefore(LocalDateTime.now().minusDays(1))) {
            accuracy = MetricAccuracy.HOURS
        }
    }

    private var timeFilter =
        Predicate { metricVos: List<MetricVo> ->
            val anyOne = metricVos[0]
            if (start != null && anyOne.createTime!!.isBefore(start)) {
                return@Predicate false
            }
            end == null || !anyOne.createTime!!.isAfter(end)
        }

    private fun filterAndMerge(metricData: Map<String, MQLVar>): Map<String, List<MetricVo>> {
        val ret: MutableMap<String, MutableList<MetricVo>> = HashMap()
        metricData.forEach { (k: String, v: MQLVar) ->
            val filtered = filter(v.data!!)
            val merged = merge(filtered)
            ret[k] = merged
        }
        return ret
    }

    private fun merge(metricData: TreeMap<String, MutableList<MetricVo>>): MutableList<MetricVo> {
        val allMetricByTagId = metricData.values.stream()
            .flatMap(Function { metricVos: List<MetricVo> ->
                metricVos.stream().map { obj: MetricVo -> obj.toTagId() }
            } as Function<List<MetricVo>, Stream<String>>)
            .collect(Collectors.toSet())

        return allMetricByTagId.stream().map { s: String ->
            val metricVos = metricData.values
                .stream().map { metricVoWithTags: List<MetricVo> ->
                    metricVoWithTags.stream()
                        .filter { metricVo: MetricVo? -> metricVo!!.toTagId() == s }
                        .findAny()
                        .orElse(null)
                }
                .filter { obj: MetricVo -> Objects.nonNull(obj) }.toList()
            if (metricVos.isEmpty()) {
                return@map null
            }
            val first = metricVos[0]
            val ret = cloneMetricVo(first!!)
            when (first.type) {
                Meter.Type.COUNTER -> ret.value = metricVos.stream().map<Double>(MetricVo::value)
                    .reduce(0.0) { a: Double, b: Double -> java.lang.Double.sum(a, b) }

                Meter.Type.GAUGE -> ret.value = metricVos[metricVos.size - 1]!!.value
                Meter.Type.TIMER -> {
                    val timerType = first.tags[MetricEnums.TimeSubType.TIMER_TYPE]
                    if (StringUtils.isBlank(timerType) || timerType == MetricEnums.TimeSubType.MAX.metricKey) {
                        // this is aggregated time-max
                        ret.value = metricVos.stream().map<Double>(MetricVo::value)
                            .reduce(0.0) { a: Double, b: Double -> java.lang.Double.max(a, b) }
                    } else if (timerType == MetricEnums.TimeSubType.TIME.metricKey || timerType == MetricEnums.TimeSubType.COUNT.metricKey) {
                        ret.value = metricVos.stream().map<Double>(MetricVo::value)
                            .reduce(0.0) { a: Double, b: Double -> java.lang.Double.sum(a, b) }
                    }
                }

                else -> {
                    throw IllegalStateException("error type:${first.type}")
                }
            }
            ret
        }.filter { obj: MetricVo? -> Objects.nonNull(obj) }.map { it!! }.toList()
    }

    private fun filter(data: TreeMap<String, MutableList<MetricVo>>): TreeMap<String, MutableList<MetricVo>> {
        val filtered = TreeMap<String, MutableList<MetricVo>>()
        data.forEach { (k: String, v: MutableList<MetricVo>) ->
            if (timeFilter.test(v)) {
                filtered[k] = v
            }
        }
        return filtered
    }

    private val isCronRunTime: Boolean
        get() {
            if (lastRun == null) {
                return true
            }
            val next = cron!!.next(lastRun!!) ?: return false
            return !next.isAfter(LocalDateTime.now())
        }

    fun setMql(mql: MQL?) {
        this.mql = mql
    }

    fun setCron(cron: CronExpression?) {
        this.cron = cron
    }

    fun setStart(start: LocalDateTime?) {
        this.start = start
    }

    fun setEnd(end: LocalDateTime?) {
        this.end = end
    }

    fun setAccuracy(accuracy: MetricAccuracy?) {
        this.accuracy = accuracy
    }

    fun setCallback(callback: Closure<*>?) {
        this.callback = callback
    }

    fun setLastRun(lastRun: LocalDateTime?) {
        this.lastRun = lastRun
    }

    fun setTimeFilter(timeFilter: Predicate<List<MetricVo>>) {
        this.timeFilter = timeFilter
    }
}
