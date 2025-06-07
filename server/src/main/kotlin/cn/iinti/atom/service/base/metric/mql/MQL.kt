package cn.iinti.atom.service.base.metric.mql

import cn.iinti.atom.service.base.metric.MetricEnums.MetricAccuracy
import cn.iinti.atom.service.base.metric.MetricService
import cn.iinti.atom.service.base.metric.mql.Context.MQLVar
import cn.iinti.atom.service.base.metric.mql.compile.MQLCompiler
import java.util.function.Function


class MQL(private val statements: List<Statement>) {
    fun run(metricAccuracy: MetricAccuracy, metricService: MetricService): Map<String, MQLVar> {
        val context = Context(metricAccuracy, metricService)
        for (statement in statements) {
            statement.run(context)
        }
        return context.exportLines
    }


    /**
     * a statement is a simple process pass of mql<br></br>
     *
     *  * simple function call
     *  * define var by response of function call
     *  * binary operator of number 、MQLVar、function call result
     *  * combine of binary operator
     *
     */
    interface Statement {
        fun run(context: Context)
    }

    /**
     * declare a mql variable and setup with an expression calculate result
     */
    class VarStatement(private val `var`: String, private val expression: Function<Context, *>) :
        Statement {
        override fun run(context: Context) {
            val mqlVar = expression.apply(context) ?: return
            check(mqlVar is MQLVar) { "the exp mast be a MQLVar" }
            context.variables[`var`] = mqlVar
        }
    }

    /**
     * just call a function, but no result setup to variable,
     * like "show(successRate)"
     */
    class VoidFunCallStatement(private val mqlFunction: Function<Context, Any?>) :
        Statement {
        override fun run(context: Context) {
            mqlFunction.apply(context)
        }
    }

    companion object {
        @JvmStatic
        fun compile(mqlCode: String): MQL {
            return MQLCompiler.compile(mqlCode)
        }
    }
}