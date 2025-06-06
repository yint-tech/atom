package cn.iinti.atom.service.base.env

import java.util.Collections
import java.util.LinkedHashSet
import java.util.Set
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

// 手动实现Lombok的Slf4j功能
private val log = org.slf4j.LoggerFactory.getLogger(ShutdownHook::class.java)

class ShutdownHook {
    companion object {
        private val shutdownHooks: Set<Runnable> = Collections.synchronizedSet(LinkedHashSet<Runnable>()) as Set<Runnable>
        @Volatile
        private var shutdownHookRunning = false

        fun registerShutdownHook(runnable: Runnable) {
            if (shutdownHookRunning) {
                throw IllegalStateException("ShutdownHook is already running")
            }
            val hookWrapper = HookWrapper(runnable)
            (shutdownHooks as MutableSet<Runnable>).add(hookWrapper)
            Runtime.getRuntime().addShutdownHook(thread(start = false) { hookWrapper.run() })
        }

        fun prepareShutdown(): Int {
            var nowTaskSize = shutdownHooks.size
            if (shutdownHookRunning || nowTaskSize == 0) {
                return nowTaskSize
            }
            synchronized(ShutdownHook::class.java) {
                if (shutdownHookRunning) {
                    return nowTaskSize
                }
                shutdownHookRunning = true
            }
            thread {
                val it = shutdownHooks.iterator()
                while (it.hasNext()) {
                    val next = it.next()
                    try {
                        next.run()
                    } catch (throwable: Throwable) {
                        log.error("running shutdown hook failed", throwable)
                    } finally {
                        it.remove()
                    }
                }
            }
            return shutdownHooks.size
        }
    }

    private class HookWrapper(private val delegate: Runnable) : Runnable {
        @Volatile
        private var called = false

        override fun run() {
            if (called) {
                return
            }
            // 使用同步块确保只执行一次
            synchronized(this) {
                if (called) {
                    return
                }
                called = true
            }
            delegate.run()
        }
    }
}