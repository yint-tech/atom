package cn.iinti.katom.entity

import cn.iinti.katom.utils.CommonUtils
import java.util.concurrent.Callable

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


        fun <T> success(t: T): CommonRes<T> {
            return CommonRes(status = statusOK, data = t)
        }

        fun <T> failed(throwable: Throwable): CommonRes<T> {
            return failed(CommonUtils.throwableToString(throwable))
        }


        fun <T> failed(message: String): CommonRes<T> {
            return failed(statusError, message)
        }


        fun <T> failed(status: Int, message: String): CommonRes<T> {
            return CommonRes(status = status, message = message)
        }


        fun <T> ofPresent(t: T?): CommonRes<T> {
            return if (t == null) {
                failed("record not found")
            } else {
                success(t)
            }
        }


        fun <T> call(callable: Callable<T>): CommonRes<T> {
            return try {
                ofPresent(callable.call())
            } catch (e: Exception) {
                failed(e)
            }
        }
    }

    fun isOk(): Boolean {
        return status == statusOK
    }

    fun <TN> errorTransfer(): CommonRes<TN> {
        return failed(status, message!!)
    }

    fun <TN> transform(function: Function<T, TN>): CommonRes<TN> {
        return if (isOk()) {
            success(function.apply(data!!))
        } else {
            errorTransfer()
        }
    }

    fun accept(consumer: (CommonRes<T>) -> Unit) {
        consumer(this)
    }

    fun ifOk(consumer: (T) -> Unit): CommonRes<T> {
        if (isOk()) {
            data?.let {
                consumer(it)
            }
        }
        return this
    }


    fun changeFailed(msg: String) {
        this.status = -1
        this.message = msg
    }
}