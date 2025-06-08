package cn.iinti.katom.base.safethread

import cn.iinti.katom.base.metric.monitor.Monitor
import io.micrometer.core.instrument.Counter
import org.slf4j.LoggerFactory
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * 单线程事件循环模型，用来避免一致性问题
 */
class Looper() : Executor {
    private val taskQueue = LinkedBlockingDeque<Runnable>()
    private lateinit var loopThread: LoopThread
    private val createTimestamp = System.currentTimeMillis()
    private var metric: Boolean = true // 默认值
    private var monitorExecuteCounter: Counter? = null
    private var monitorLooperQueueSize: AtomicInteger? = null

    // 主构造函数
    constructor(looperName: String, metric: Boolean = true) : this() {
        this.loopThread = LoopThread(looperName)
        this.metric = metric
        if (metric) {
            monitorExecuteCounter = Monitor.counter("looper.execute.$looperName")
            monitorLooperQueueSize = AtomicInteger(0)
            Monitor.gauge("looper.taskQueueSize.$looperName", monitorLooperQueueSize!!)
        }
    }

    companion object {
        @Volatile
        private var lowPriorityLooper: Looper? = null

        /**
         * 获取一个全局的低优looper
         *
         * @return looper
         */
        fun getLowPriorityLooper(): Looper {
            return synchronized(this) {
                if (lowPriorityLooper == null) {
                    lowPriorityLooper = Looper("lowPriorityLooper").startLoop()
                }
                lowPriorityLooper!!
            }
        }

        /**
         * 让looper拥有延时任务的能力
         */
        private val scheduler = Executors.newScheduledThreadPool(1)

        private val logger = LoggerFactory.getLogger(Looper::class.java)
    }

    fun startLoop(): Looper {
        loopThread.start()
        return this
    }

    override fun execute(command: Runnable) {
        if (inLooper()) {
            command.run()
            return
        }
        post(command)
    }

    fun post(runnable: Runnable) {
        post(runnable, false)
    }

    fun offerLast(runnable: Runnable) {
        taskQueue.addLast(runnable)
    }

    fun post(runnable: Runnable, first: Boolean) {
        if (!loopThread.isAlive) {
            if (System.currentTimeMillis() - createTimestamp > 60000) {
                logger.warn("post task before looper startup, do you call :startLoop??")
            }
            runnable.run()
            return
        }
        if (first) {
            taskQueue.offerFirst(runnable)
        } else {
            taskQueue.add(runnable)
        }
    }

    // 将 postDelay 设为 public 以供外部访问
    fun postDelay(runnable: Runnable, delay: Long) {
        if (delay <= 0) {
            post(runnable)
            return
        }
        if (!loopThread.isAlive) {
            //todo 这是应该有bug，先加上这一行日志
            if (System.currentTimeMillis() - createTimestamp > 60000) {
                logger.warn("post task before looper startup, do you call :startLoop??")
            }
        }
        scheduler.schedule({ post(runnable) }, delay, TimeUnit.MILLISECONDS)
    }

    fun fluentScheduleWithRate(rate: Long, runnable: Runnable): Looper {
        scheduleWithRate(rate, runnable)
        return this
    }

    fun scheduleWithRate(rate: Number, runnable: Runnable): FixRateScheduleHandle {
        val fixRateScheduleHandle = FixRateScheduleHandle(runnable, rate)
        postDelay(fixRateScheduleHandle, rate.toLong())
        return fixRateScheduleHandle
    }

    /**
     * 这个接口，可以支持非固定速率
     */
    inner class FixRateScheduleHandle(private val runnable: Runnable, private val rate: Number) : Runnable {
        @Volatile
        private var running = true

        fun cancel() {
            running = false
        }

        override fun run() {
            if (running && rate.toLong() > 0) {
                postDelay(this, rate.toLong())
            }
            runnable.run()
        }
    }

    private inner class LoopThread(name: String) : Thread(null, null, name) {
        override fun run() {
            while (!Thread.currentThread().isInterrupted) {
                try {
                    val runnable = taskQueue.take()
                    if (metric) {
                        monitorExecuteCounter?.increment()
                        monitorLooperQueueSize?.set(taskQueue.size)
                    }
                    runnable.run()
                } catch (interruptedException: InterruptedException) {
                    return
                } catch (throwable: Throwable) {
                    logger.error("group event loop error", throwable)
                }
            }
        }
    }

    fun inLooper(): Boolean {
        return Thread.currentThread() === loopThread || !loopThread.isAlive
    }

    fun checkLooper() {
        if (!inLooper()) {
            throw IllegalStateException("run task not in looper")
        }
    }

    fun close() {
        loopThread.interrupt()
    }
}