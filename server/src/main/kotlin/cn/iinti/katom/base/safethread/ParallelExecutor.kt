package cn.iinti.katom.base.safethread

class ParallelExecutor<T>(private val looper: Looper, private val eventSize: Int, private val parallelConnectEvent: ParallelConnectEvent<T>) :
    ValueCallback<T> {
    private var eventIndex = 0
    private var success = false

    override fun onReceiveValue(value: Value<T>) {
        if (!looper.inLooper()) {
            looper.post { onReceiveValue(value) }
            return
        }

        eventIndex++

        if (value.isSuccess()) {
            if (!success) {
                success = true
                parallelConnectEvent.firstSuccess(value)
            } else {
                parallelConnectEvent.secondSuccess(value)
            }
            return
        }

        if (!success && eventIndex >= eventSize) {
            parallelConnectEvent.finalFailed(value.e!!)
        }
    }

    interface ParallelConnectEvent<T> {
        fun firstSuccess(value: Value<T>)

        fun secondSuccess(value: Value<T>)

        fun finalFailed(throwable: Throwable)
    }
}