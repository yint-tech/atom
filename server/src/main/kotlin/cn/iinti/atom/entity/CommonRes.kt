package cn.iinti.atom.entity

import cn.iinti.atom.utils.CommonUtils
import java.util.concurrent.Callable
import java.util.function.Consumer
import java.util.function.Function

data class CommonRes<T>(
    var status: Int = statusOK,
    var message: String? = null,
    var data: T? = null
) {

    companion object {
        const val statusOK = 0
        const val statusError = -1
        const val statusBadRequest = -2
        const val statusNeedLogin = -4
        const val statusLoginExpire = -5
        const val statusDeny = -6

        @JvmStatic
        fun <T> success(t: T): CommonRes<T> {
            return CommonRes(status = statusOK, data = t)
        }

        @JvmStatic
        fun <T> failed(throwable: Throwable): CommonRes<T> {
            return failed(CommonUtils.throwableToString(throwable))
        }

        @JvmStatic
        fun <T> failed(message: String): CommonRes<T> {
            return failed(statusError, message)
        }

        @JvmStatic
        fun <T> failed(status: Int, message: String): CommonRes<T> {
            return CommonRes(status = status, message = message)
        }

        @JvmStatic
        fun <T> ofPresent(t: T?): CommonRes<T> {
            return if (t == null) {
                CommonRes.failed("record not found")
            } else {
                CommonRes.success(t)
            }
        }

        @JvmStatic
        fun <T> call(callable: Callable<T>): CommonRes<T> {
            return try {
                ofPresent(callable.call())
            } catch (e: Exception) {
                CommonRes.failed(e)
            }
        }
    }

    fun isOk(): Boolean {
        return status == statusOK
    }

    fun <TN> errorTransfer(): CommonRes<TN> {
        return CommonRes.failed(status, message!!)
    }

    fun <TN> transform(function: Function<T, TN>): CommonRes<TN> {
        return if (isOk()) {
            CommonRes.success(function.apply(data!!))
        } else {
            errorTransfer()
        }
    }

    fun accept(consumer: Consumer<CommonRes<T>>) {
        consumer.accept(this)
    }

    fun ifOk(consumer: Consumer<T>): CommonRes<T> {
        if (isOk()) {
            data?.let(consumer::accept)
        }
       return this
    }

    fun acceptIfOk(consumer: Consumer<CommonRes<T>>) {
        if (isOk()) {
            consumer.accept(this)
        }
    }

    fun <NT> callIfOk(callable: Callable<CommonRes<NT>>): CommonRes<NT> {
        return if (isOk()) {
            try {
                callable.call()
            } catch (e: Exception) {
                CommonRes.failed(e)
            }
        } else {
            errorTransfer()
        }
    }

    fun changeFailed(msg: String) {
        this.status = -1
        this.message = msg
    }
}