package cn.iinti.katom.base.metric.mql.func

import cn.iinti.katom.base.metric.mql.Context
import cn.iinti.katom.base.metric.mql.Context.MQLVar
import cn.iinti.katom.base.metric.mql.compile.MQLCompiler.BadGrammarException
import com.google.common.collect.Maps
import lombok.SneakyThrows
import org.apache.commons.lang3.StringUtils
import java.lang.reflect.Constructor
import java.util.function.Function


abstract class MQLFunction(protected var params: List<String>) {
    /**
     * call this function, the result is a new Context.MQLVar node
     */
    abstract fun call(context: Context): MQLVar?

    /**
     * any mql function can be a supplier of MetricOperator param,
     * and then the result of function call can be add、minus、multiple、divide
     */
    fun asOpNode(): Function<Context, Any?> {
        return Function { context: Context -> this.call(context) }
    }


    @Target(AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class MQL_FUNC(
        /**
         * @return the name of this function
         */
        val value: String
    )

    companion object {
        private var functionRegistry: MutableMap<String, Constructor<out MQLFunction?>> = Maps.newHashMap()

        init {
            registryFunc(FuncAggregate::class.java)
            registryFunc(FuncFilter::class.java)
            registryFunc(FuncMetric::class.java)
            registryFunc(FuncShow::class.java)
            registryFunc(FuncGetVar::class.java)
            registryFunc(FuncShift::class.java)
            registryFunc(FuncDropLeft::class.java)
            registryFunc(FuncTopN::class.java)
        }

        @SneakyThrows
        private fun registryFunc(clazz: Class<out MQLFunction?>) {
            val mqlFunc = clazz.getAnnotation(
                MQL_FUNC::class.java
            )
            checkNotNull(mqlFunc) { "error function registry for class: $clazz" }
            val funcName = mqlFunc.value
            if (StringUtils.isBlank(funcName)) {
                throw RuntimeException("empty funcName")
            }
            check(!functionRegistry.containsKey(funcName)) { "duplicate registry for function: $funcName" }
            val constructor = clazz.getConstructor(MutableList::class.java)
            functionRegistry[mqlFunc.value] = constructor
        }

        @SneakyThrows
        fun createFunction(name: String, params: List<String?>?): MQLFunction {
            val constructor = functionRegistry[name]
                ?: throw BadGrammarException("no function: $name defined")
            return constructor.newInstance(params)!!
        }

        fun isFunctionNotDefined(name: String): Boolean {
            return !functionRegistry.containsKey(name)
        }
    }
}
