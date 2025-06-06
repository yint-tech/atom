package cn.iinti.atom.service.base.safethread

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread

class ValueCallbackGetter<T> {
    @Volatile
    private var value: Value<T>? = null
    private val lock = Object()
    
    // 手动实现的getter
    val callback: ValueCallback<T>
        get() = object : ValueCallback<T> {
            override fun onReceiveValue(value: Value<T>) {
                this@ValueCallbackGetter.value = value
                synchronized(lock) {
                    lock.notifyAll()
                }
            }
        }

    fun getUncheck(): T {
        return try {
            get()
        } catch (throwable: Throwable) {
            throw IllegalStateException(throwable)
        }
    }

    @Throws(Throwable::class)
    fun get(): T {
        if (value == null) {
            synchronized(lock) {
                if (value == null) {
                    // 简单等待，实际应使用更可靠的同步机制
                    lock.wait(1000)
                }
            }
        }
        return if (value?.isSuccess() == true) {
            value!!.v!!
        } else {
            throw value!!.e!!
        }
    }

    interface Setup<T> {
        fun setup(callback: ValueCallback<T>)
    }

    companion object {
        fun <T> syncGet(setup: Setup<T>): T {
            val callbackGetter = ValueCallbackGetter<T>()
            setup.setup(callbackGetter.callback)
            return callbackGetter.getUncheck()
        }
    }
}