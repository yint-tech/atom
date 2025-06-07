package cn.iinti.atom.service.base.metric.embed

import cn.iinti.atom.service.base.config.Configs.IntegerConfigValue
import cn.iinti.atom.service.base.config.Configs.addKeyMonitor
import cn.iinti.atom.service.base.metric.monitor.Monitor.counter
import cn.iinti.atom.service.base.metric.monitor.Monitor.gauge
import cn.iinti.atom.service.base.metric.monitor.Monitor.timer
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.Timer
import jakarta.validation.constraints.NotNull
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Function


/**
 * a framework custom threadPool
 *
 *  * will be monitor by metric component
 *  * can be reset thread size by cloud config
 *
 */
class MonitorThreadPoolExecutor : ThreadPoolExecutor {
    private val name: String
    private var timeMonitor: Timer? = null
    private var exceptionCounter: Counter? = null

    constructor(
        corePoolSize: Int,
        maximumPoolSize: Int,
        name: String,
        keepAliveTime: Long,
        unit: TimeUnit,
        workQueue: BlockingQueue<Runnable?>,
        handler: RejectedExecutionHandler
    ) : super(
        corePoolSize, maximumPoolSize,
        keepAliveTime, unit, workQueue,
        NamedThreadFactory(name),
        MonitorRejectedExecutionHandler(handler, name)
    ) {
        this.name = name
        // all metric monitor of this threadPool
        setupMonitor()
    }

    @JvmOverloads
    constructor(
        threadSizeConfig: IntegerConfigValue,
        name: String,
        workQueue: BlockingQueue<Runnable?>,
        handler: RejectedExecutionHandler = AbortPolicy()
    ) : super(
        threadSizeConfig.value!!, threadSizeConfig.value!!,
        0, TimeUnit.MINUTES, workQueue,
        NamedThreadFactory(name),
        MonitorRejectedExecutionHandler(handler, name)
    ) {
        this.name = name
        // all metric monitor of this threadPool
        setupMonitor()

        // re config thread size from configs
        linkThreadSizeConfig(threadSizeConfig)
    }

    override fun execute(command: @NotNull Runnable) {
        super.execute(timeMonitor!!.wrap(command))
    }

    override fun afterExecute(r: Runnable, t: Throwable?) {
        super.afterExecute(r, t)
        if (t != null) {
            exceptionCounter!!.increment()
        }
    }

    private fun linkThreadSizeConfig(threadSizeConfig: IntegerConfigValue) {
        addKeyMonitor(threadSizeConfig.key, {
            val newThreadSize = threadSizeConfig.value
            val corePoolSize = corePoolSize
            if (newThreadSize!! < corePoolSize) {
                setCorePoolSize(newThreadSize)
                maximumPoolSize = newThreadSize
            } else if (newThreadSize > corePoolSize) {
                maximumPoolSize = newThreadSize
                setCorePoolSize(newThreadSize)
            }
        })
    }

    private fun setupMonitor() {
        registerGauge("coreSize") { it: MonitorThreadPoolExecutor -> it.corePoolSize.toDouble() }
        registerGauge("activeCount") { it: MonitorThreadPoolExecutor -> it.activeCount.toDouble() }
        registerGauge("queueSize") { it: MonitorThreadPoolExecutor -> it.queue.size.toDouble() }
        timeMonitor = timer(METRIC_NAME_TIMER, "name", name)
        exceptionCounter = counter(METRIC_NAME_COUNT, "name", name, "type", "exception")
    }

    private fun registerGauge(type: String, function: Function<MonitorThreadPoolExecutor, Double>) {
        gauge(
            METRIC_NAME_GAUGE,
            Tags.of("name", name, "type", type),
            this
        ) { t: MonitorThreadPoolExecutor -> function.apply(t) }
    }

    private class MonitorRejectedExecutionHandler(private val delegate: RejectedExecutionHandler, threadName: String?) :
        RejectedExecutionHandler {
        private val rejectCounter: Counter = counter(
            METRIC_NAME_COUNT, "name",
            threadName!!, "type", "reject"
        )

        override fun rejectedExecution(r: Runnable, executor: ThreadPoolExecutor) {
            rejectCounter.increment()
            delegate.rejectedExecution(r, executor)
        }
    }


    class NamedThreadFactory(private val prefix: String) : ThreadFactory {
        private val sequence = AtomicInteger(1)

        override fun newThread(r: Runnable): Thread {
            val thread = Thread(r)
            val seq = sequence.getAndIncrement()
            thread.name = prefix + (if (seq > 1) "-$seq" else "")
            if (!thread.isDaemon) thread.isDaemon = true
            return thread
        }
    }

    companion object {
        private const val METRIC_NAME_GAUGE = "thread.pool.gauge"
        private const val METRIC_NAME_COUNT = "thread.pool.count"
        private const val METRIC_NAME_TIMER = "thread.pool.timer"
    }
}
