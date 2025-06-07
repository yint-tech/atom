package cn.iinti.atom.system

import cn.iinti.atom.entity.CommonRes
import org.apache.catalina.connector.ClientAbortException
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException
import proguard.annotation.Keep
import org.slf4j.LoggerFactory

/**
 * Date: 2021-06-02
 *
 * @author alienhe
 */
@RestControllerAdvice
@Keep
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(
        value = [
            MethodArgumentNotValidException::class,
            IllegalArgumentException::class
        ]
    )
    fun handleMethodArgumentNotValid(exception: Exception): CommonRes<String> {
        if (exception is MethodArgumentNotValidException) {
            if (exception.bindingResult.fieldError != null) {
                return CommonRes.failed(
                    "参数错误：" + exception.bindingResult
                        .fieldError?.defaultMessage
                )
            }
        }

        log.error("GlobalException", exception)
        return CommonRes.failed("参数错误：" + exception.message)
    }

    @ExceptionHandler(value = [Exception::class])
    fun handleUncaughtException(exception: Exception): CommonRes<String> {
        if (exception is NoResourceFoundException) {
            throw exception
        }
        if (isClientAbortException(exception)) {
            // ClientAbortException不打印异常级别日志
            log.info("client abort", exception)
            return CommonRes.failed("unexpected error:" + exception.message)
        }
        log.error("unexpected exception:", exception)
        return CommonRes.failed("unexpected error:" + exception.message)
    }

    private fun isClientAbortException(e: Throwable?): Boolean {
        if (e == null) {
            return false
        }
        if (e is ClientAbortException) {
            return true
        }
        if (e is HttpMessageNotReadableException) {
            return true
        }

        return isClientAbortException(e.cause)
    }
}