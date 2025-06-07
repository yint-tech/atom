package cn.iinti.atom.service.base.metric.monitor

import cn.iinti.atom.service.base.metric.embed.DefaultMetricSetup
import com.google.common.util.concurrent.AtomicDouble
import io.micrometer.core.instrument.*
import io.micrometer.core.lang.Nullable
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.function.ToDoubleFunction


class Monitor {
    companion object {
        init {
            DefaultMetricSetup.setup()
        }

        private val log = LoggerFactory.getLogger(Monitor::class.java)

        fun addRegistry(registry: MeterRegistry) {
            Metrics.addRegistry(registry)
        }

        fun removeRegistry(registry: MeterRegistry) {
            Metrics.removeRegistry(registry)
        }

        fun counter(name: String, tags: Iterable<Tag>): Counter {
            return Metrics.counter(name, tags)
        }

        fun counter(name: String, vararg tags: String): Counter {
            if (!checkStringTags(*tags)) {
                return Metrics.counter(name)
            }
            return Metrics.counter(name, *tags)
        }

        fun summary(name: String, tags: Iterable<Tag>): DistributionSummary {
            return Metrics.summary(name, tags)
        }

        fun summary(name: String, vararg tags: String): DistributionSummary {
            if (!checkStringTags(*tags)) {
                return Metrics.summary(name)
            }
            return Metrics.summary(name, *tags)
        }

        fun timer(name: String, tags: Iterable<Tag>): Timer {
            return Metrics.timer(name, tags)
        }

        fun timer(name: String, vararg tags: String): Timer {
            if (!checkStringTags(*tags)) {
                return Metrics.timer(name)
            }
            return Metrics.timer(name, *tags)
        }

        fun more(): Metrics.More {
            return Metrics.more()
        }

        @Nullable
        fun <T> gauge(name: String, tags: Iterable<Tag>, obj: T, valueFunction: ToDoubleFunction<T>): T {
            return Metrics::class.java.getMethod("gauge", String::class.java, Iterable::class.java, Any::class.java, ToDoubleFunction::class.java)
                .invoke(null, name, tags, obj, valueFunction) as T
        }

        @Nullable
        fun <T : Number> gauge(name: String, tags: Iterable<Tag>, number: T): T {
            return Metrics::class.java.getMethod("gauge", String::class.java, Iterable::class.java, Any::class.java)
                .invoke(null, name, tags, number) as T
        }

        private val fastGaugeMap = ConcurrentHashMap<Meter.Id, AtomicDouble>()

        fun gauge(name: String, tags: Iterable<Tag>): AtomicDouble {
            val id = Meter.Id(name, Tags.of(tags), null, "fast", Meter.Type.GAUGE)
            return fastGaugeMap.computeIfAbsent(id) { id1 ->
                val gauge = AtomicDouble(0.0)
                return@computeIfAbsent gauge(name, tags, gauge)
            }!!
        }

        @Nullable
        fun <T : Number> gauge(name: String, number: T): T {
            return Metrics.gauge(name, number)
        }

        @Nullable
        fun <T> gauge(name: String, obj: T, valueFunction: ToDoubleFunction<T>): T {
            return Metrics.gauge(name, obj, valueFunction)
        }

        @Nullable
        fun <T : kotlin.collections.Collection<*>?> gaugeCollectionSize(
            name: String,
            tags: Iterable<Tag?>,
            collection: T
        ): T {
            return Metrics.gaugeCollectionSize(name, tags, collection)
        }

        @Nullable
        fun <T : kotlin.collections.Map<*, *>?> gaugeMapSize(name: String, tags: Iterable<Tag?>, map: T): T {
            return Metrics.gaugeMapSize(name, tags, map)
        }


        private fun checkStringTags(vararg tags: String): Boolean {
            if (tags == null) {
                return true
            }
            if (tags.size % 2 != 0) {
                // 这么做的原因是，对于监控系统，即使是指标错误，理论他的api不应该报错
                // 无论如何他不能干扰业务逻辑
                counter("monitor_tags_num_error").increment()
                log.error("tags num error:{}", tags.joinToString(","), Throwable())
                return false
            }
            return true
        }
    }
}