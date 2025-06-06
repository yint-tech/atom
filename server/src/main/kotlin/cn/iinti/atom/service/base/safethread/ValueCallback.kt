package cn.iinti.atom.service.base.safethread

interface ValueCallback<T> {
    fun onReceiveValue(value: Value<T>)
}

// 包级函数作为静态方法的替代
fun <T> success(callback: ValueCallback<T>, t: T) {
    callback.onReceiveValue(Value.success(t))
}

fun <T> failed(callback: ValueCallback<T>, e: Throwable) {
    callback.onReceiveValue(Value.failed(e))
}

fun <T> failed(callback: ValueCallback<T>, message: String) {
    callback.onReceiveValue(Value.failed(message))
}

class Value<T> {
    var v: T? = null
    var e: Throwable? = null

    fun isSuccess(): Boolean {
        return e == null
    }

    companion object {
        @JvmStatic
        fun <T> failed(e: Throwable): Value<T> {
            val value = Value<T>()
            value.e = e
            return value
        }

        @JvmStatic
        fun <T> failed(message: String): Value<T> {
            return failed(RuntimeException(message))
        }

        @JvmStatic
        fun <T> success(t: T): Value<T> {
            val value = Value<T>()
            value.v = t
            return value
        }
    }

    fun <N> errorTransfer(): Value<N> {
        val value = Value<N>()
        value.e = e
        return value
    }
}
