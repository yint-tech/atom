package cn.iinti.katom.base.metric.embed

import cn.iinti.katom.base.config.Settings
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.core.instrument.binder.jvm.*
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics

object DefaultMetricSetup {
    private val jvmGcMetrics = JvmGcMetrics()

    fun setup() {
        // 系统级别的默认指标，内存/CPU/线程等
        bind(DiskSpaceMetrics(Settings.Storage.root))
        bind(JvmThreadMetrics())
        bind(JvmMemoryMetrics())
        bind(ClassLoaderMetrics())
        bind(jvmGcMetrics)
        bind(FileDescriptorMetrics())
        bind(ProcessorMetrics())
    }

    private fun bind(binder: MeterBinder) {
        binder.bindTo(Metrics.globalRegistry)
    }
}