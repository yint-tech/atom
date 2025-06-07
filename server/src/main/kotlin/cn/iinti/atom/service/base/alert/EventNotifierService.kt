package cn.iinti.atom.service.base.alert

import cn.iinti.atom.service.base.alert.events.DiskPoorEvent
import cn.iinti.atom.service.base.alert.events.SensitiveOperationEvent
import cn.iinti.atom.service.base.config.Settings
import cn.iinti.atom.service.base.metric.MetricService
import cn.iinti.atom.service.base.safethread.Looper
import cn.iinti.atom.utils.ServerIdentifier
import groovy.lang.Closure
import jakarta.annotation.Resource
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Service

@Service
class EventNotifierService : CommandLineRunner {
    private val eventNotifierThread = Looper("eventNotifier").startLoop()

    @Resource
    lateinit var metricService: MetricService

    private var metricNotifyRecord: MutableMap<String, MetricMonitorHandle> = LinkedHashMap()

    fun notifySensitiveOperation(user: String, api: String, params: String) {
        withScriptExtension { eventScript ->
            val event = SensitiveOperationEvent(user, api, params)
            callExtensions(eventScript.sensitiveEventList, event)
        }
    }

    private fun scheduleDiskSpacePoorEvent() {
        val root = Settings.Storage.root
        val totalSpace = root.totalSpace
        val freeSpace = root.freeSpace
        if (freeSpace * 100 / totalSpace > 25) {
            return
        }
        withScriptExtension { eventScript ->
            val diskPoorEvent = DiskPoorEvent(totalSpace, freeSpace, ServerIdentifier.id())
            callExtensions(eventScript.diskPoorEventList, diskPoorEvent)
        }
    }

    fun scheduleMetricEvent(force: Boolean) {
        withScriptExtension { eventScript ->
            val newRecords: MutableMap<String, MetricMonitorHandle> = HashMap()
            eventScript.metricMonitorConfigList.forEach { config ->
                val metricMonitorHandle =
                    metricNotifyRecord.computeIfAbsent(config.getId()) { _ -> MetricMonitorHandle() }

                config.fillMeta(metricMonitorHandle)
                try {
                    metricMonitorHandle.evaluate(metricService, force)
                } catch (e: Exception) {
                    // Handle exception
                }
                newRecords[config.getId()] = metricMonitorHandle
            }
            metricNotifyRecord = newRecords
        }
    }

    private fun callExtensions(eventList: List<Closure<*>>, delegate: Any) {
        for (closure in eventList) {
            try {
                closure.rehydrate(delegate, closure.owner, closure.delegate)
                    .call()
            } catch (e: Exception) {
                // Handle exception
            }
        }
    }

    private fun withScriptExtension(func: (EventScript) -> Unit) {
        val eventScript = Settings.eventNotifyScript.value ?: return
        eventNotifierThread.post { func(eventScript) }
    }

    override fun run(vararg args: String?) {
        eventNotifierThread.scheduleWithRate(30 * 60 * 1000) { scheduleDiskSpacePoorEvent() }
        eventNotifierThread.scheduleWithRate(10 * 60 * 1000) { scheduleMetricEvent(false) }
    }
}