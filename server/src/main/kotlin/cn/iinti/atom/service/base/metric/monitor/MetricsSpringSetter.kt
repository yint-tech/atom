package cn.iinti.atom.service.base.metric.monitor

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Service


/**
 * 将MeterRegistry注入到全局，以便通过静态api记录指标，
 * 在没有spring环境的时候，本类不生效
 */
@Service
class MetricsSpringSetter : ApplicationContextAware {
    @Throws(BeansException::class)
    override fun setApplicationContext(applicationContext: ApplicationContext) {
        try {
            val meterRegistry = applicationContext.getBean(MeterRegistry::class.java)
            Monitor.addRegistry(meterRegistry)
        } catch (ignore: BeansException) {
        }
    }
}
