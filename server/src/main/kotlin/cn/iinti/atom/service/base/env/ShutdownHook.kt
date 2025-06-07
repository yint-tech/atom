package cn.iinti.atom.service.base.env

import java.util.*
import kotlin.collections.LinkedHashSet
import kotlin.concurrent.thread

// 手动实现Lombok的Slf4j功能
private val log = org.slf4j.LoggerFactory.getLogger(ShutdownHook::class.java)

class ShutdownHook {
    companion object {
        private val shutdownHooks: MutableSet<Runnable> =
            Collections.synchronizedSet(LinkedHashSet<Runnable>())

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
            val nowTaskSize = shutdownHooks.size
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
                val clean = mutableSetOf<Runnable>()
                for (shutdownHook in shutdownHooks) {
                    try {
                        shutdownHook.run()
                    } catch (throwable: Throwable) {
                        log.error("running shutdown hook failed", throwable)
                    } finally {
                        clean.add(shutdownHook)
                    }
                }
                shutdownHooks.removeAll(clean)
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