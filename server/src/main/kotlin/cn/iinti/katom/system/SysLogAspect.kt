package cn.iinti.katom.system

import cn.iinti.katom.entity.SysLog
import cn.iinti.katom.mapper.SysLogMapper
import cn.iinti.katom.base.alert.EventNotifierService
import jakarta.annotation.Resource
import org.apache.commons.lang3.StringUtils
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.stereotype.Component
import java.lang.reflect.Method
import java.util.*

@Aspect
@Component
class SysLogAspect {
    @Resource
    private lateinit var sysLogMapper: SysLogMapper

    @Resource
    private lateinit var eventNotifierService: EventNotifierService

    // 手动实现Lombok的@Slf4j功能
    private val log = org.slf4j.LoggerFactory.getLogger(SysLogAspect::class.java)

    @Around("@annotation(cn.iinti.katom.system.LoginRequired)")
    @Throws(Throwable::class)
    fun around(point: ProceedingJoinPoint): Any? {
        val result = point.proceed()
        // 保存日志
        saveLog(point)
        return result
    }

    private fun saveLog(point: ProceedingJoinPoint) {
        val user = AppContext.getUser()
        if (user == null || AppContext.isApiUser()) {
            return
        }

        val loginAnnotation = AppContext.getLoginAnnotation()
        if (loginAnnotation?.skipLogRecord == true) {
            return
        }

        val signature = point.signature as MethodSignature
        val className = point.target.javaClass.name
        val methodName = signature.name
        val target = "$className#$methodName"

        val method: Method = signature.method
        val sysLog = SysLog()
        sysLog.username = AppContext.getUser()!!.userName
        sysLog.operation = method.name
        val params = Arrays.toString(point.args)
        sysLog.params = StringUtils.substring(params, 0, 50)

        sysLog.methodName = target
        log.info("record sys log:{}", sysLog)
        sysLogMapper.insert(sysLog)

        // alert
        if (loginAnnotation?.alert == true) {
            eventNotifierService.notifySensitiveOperation(
                sysLog.username!!,
                target,
                sysLog.params!!
            )
        }
    }
}

