package cn.iinti.atom.service.base.alert.events

import cn.iinti.atom.service.base.alert.MetricMonitorHandle
import cn.iinti.atom.service.base.metric.MetricEnums
import cn.iinti.atom.service.base.metric.MetricVo
import cn.iinti.atom.service.base.metric.mql.MQL
import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.apache.commons.lang3.StringUtils
import org.springframework.scheduling.support.CronExpression
import java.time.LocalDateTime
import java.util.*

class MetricMonitorConfig {
    private var _id: String = UUID.randomUUID().toString()
    private var _cron: CronExpression? = null
    private var _mql: MQL? = null
    private var _start: LocalDateTime? = null
    private var _end: LocalDateTime? = null
    private var _accuracy: MetricEnums.MetricAccuracy = MetricEnums.MetricAccuracy.hours

    private var callback: Closure<*>? = null

    fun id(id: String) {
        this._id = id
    }

    fun getId(): String {
        return this._id
    }

    fun cron(con: String) {
        this._cron = CronExpression.parse(con)
    }

    fun mql(mql: String) {
        this._mql = MQL.compile(mql)
    }

    fun start(start: LocalDateTime) {
        this._start = start
    }

    fun end(end: LocalDateTime) {
        this._end = end
    }

    fun accuracy(accuracy: MetricEnums.MetricAccuracy) {
        this._accuracy = accuracy
    }

    class MetricData(private val metric: Map<String, List<MetricVo>>) {
        fun getMetricValue(key: String, vararg tags: String): Double {
            val value = getMetric(key, *tags)
            return value ?: 0.0
        }

        fun getMetric(key: String, vararg tags: String): Double? {
            val metricVos = metric[key]
            if (metricVos == null || metricVos.isEmpty()) {
                return null
            }
            if (metricVos.size == 1) {
                return metricVos[0].value
            }
            // filter by tags
            val tagMap = HashMap<String, String>()
            for (i in tags.indices step 2) {
                if (i + 1 < tags.size) {
                    tagMap[tags[i]] = tags[i + 1]
                }
            }
            for (metricVo in metricVos) {
                if (isTagEquals(metricVo.tags, tagMap)) {
                    return metricVo.value
                }
            }
            // warning
            return null
        }

        companion object {
            fun isTagEquals(tags1: Map<String, String>, tags2: Map<String, String>): Boolean {
                if (tags1.size != tags2.size) {
                    return false
                }
                for ((k, v) in tags1) {
                    if (!StringUtils.equals(v, tags2[k])) {
                        return false
                    }
                }
                return true
            }
        }
    }

    fun valid() {
        if (_mql == null) {
            throw IllegalStateException("mql not set")
        }
        if (_cron == null) {
            throw IllegalStateException("cron not set")
        }
        if (callback == null) {
            throw IllegalStateException("onMetric callback is null")
        }
    }

    fun onMetric(@DelegatesTo(MetricData::class) closure: Closure<*>) {
        callback = closure
    }

    fun fillMeta(metricMonitorHandle: MetricMonitorHandle) {
        metricMonitorHandle.setMql(_mql)
        metricMonitorHandle.setCron(_cron)
        metricMonitorHandle.setCallback(callback)
        metricMonitorHandle.setStart(_start)
        metricMonitorHandle.setEnd(_end)
        metricMonitorHandle.setAccuracy(_accuracy)
    }
}