package cn.iinti.katom.base.metric.mql.func

import cn.iinti.katom.base.metric.mql.Context
import cn.iinti.katom.base.metric.mql.Context.MQLVar
import cn.iinti.katom.base.metric.mql.func.MQLFunction.MQL_FUNC


@MQL_FUNC("getVar")
class FuncGetVar(params: List<String>) : MQLFunction(params) {
    private val varName = params[0]

    override fun call(context: Context): MQLVar? {
        val mqlVar = context.variables[varName] ?: return null
        return mqlVar.copy()
    }
}
