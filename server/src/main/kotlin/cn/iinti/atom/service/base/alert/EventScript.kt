package cn.iinti.atom.service.base.alert

import cn.iinti.atom.service.base.BroadcastService.Companion.post
import cn.iinti.atom.service.base.alert.events.DiskPoorEvent
import cn.iinti.atom.service.base.alert.events.MetricMonitorConfig
import cn.iinti.atom.service.base.alert.events.SensitiveOperationEvent
import cn.iinti.atom.utils.net.SimpleHttpInvoker
import com.alibaba.fastjson.JSONObject
import groovy.json.JsonBuilder
import groovy.lang.Closure
import groovy.lang.DelegatesTo
import groovy.lang.GroovyShell
import groovy.lang.Script
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.SecureASTCustomizer
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.Socket
import java.net.URLConnection
import java.nio.file.Files
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocket


abstract class EventScript : Script() {
    var sensitiveEventList: MutableList<Closure<*>> = ArrayList()

    var diskPoorEventList: MutableList<Closure<*>> = ArrayList()

    var metricMonitorConfigList: MutableList<MetricMonitorConfig> = ArrayList()

    /**
     * 敏感操作事件，被标记了alert的接口，当存在用户对他进行访问时，此扩展脚本将会被调用
     *
     * @see cn.iinti.atom.system.LoginRequired
     */
    fun onSensitiveOperation(@DelegatesTo(SensitiveOperationEvent::class) closure: Closure<*>) {
        sensitiveEventList.add(closure)
    }

    /**
     * 当系统磁盘少于75%的时候，此扩展接口将会被调用
     */
    fun onDiskPoor(@DelegatesTo(DiskPoorEvent::class) closure: Closure<*>) {
        diskPoorEventList.add(closure)
    }


    fun mqlSubscribe(@DelegatesTo(MetricMonitorConfig::class) closure: Closure<*>) {
        val metricMonitorConfig = MetricMonitorConfig()
        closure.rehydrate(metricMonitorConfig, closure.owner, closure.thisObject)
            .call()
        metricMonitorConfig.valid()
        metricMonitorConfigList.add(metricMonitorConfig)
    }

    fun httpPost(url: String, jsonBuilder: JsonBuilder): String {
        return SimpleHttpInvoker.post(url, JSONObject.parseObject(jsonBuilder.toString()))
    }


    companion object {
        fun compileScript(groovyScriptSource: String?): EventScript {
            // 控制脚本的行为，有一些api不让脚本访问，特别是命令行，文件等
            val secureASTCustomizer = SecureASTCustomizer()
            val disallow: MutableList<Class<*>> = ArrayList()
            disallow.add(File::class.java) // 如果开放文件读写，则可能让他们直接读写操作系统的文件，很多同学都是直接使用root运行代码的
            disallow.add(Files::class.java)
            disallow.add(FileInputStream::class.java)
            disallow.add(FileOutputStream::class.java)
            disallow.add(Runtime::class.java) // 同理，exec函数在这里实现
            disallow.add(URLConnection::class.java) // 不允许他直接访问网络，这可能导致框架不稳定，毕竟网络操作非常耗时
            disallow.add(HttpsURLConnection::class.java)
            disallow.add(Socket::class.java) // 同理
            try {
                disallow.add(SSLSocket::class.java)
            } catch (ignore: Throwable) {
                //ignore
            }
            try {
                // fastjson的漏洞，通过他加载rmi，然后被shell注入了
                val JdbcRowSetImplClass = ClassLoader.getSystemClassLoader().loadClass("com.sun.rowset.JdbcRowSetImpl")
                disallow.add(JdbcRowSetImplClass)
            } catch (ignore: Throwable) {
                //ignore
            }
            secureASTCustomizer.setDisallowedReceiversClasses(disallow)

            val configuration = CompilerConfiguration()
            configuration.addCompilationCustomizers(secureASTCustomizer)
            configuration.scriptBaseClass = EventScript::class.java.name
            val shell = GroovyShell(configuration)
            val t = shell.parse(groovyScriptSource) as EventScript
            t.run()
            return t
        }
    }
}
