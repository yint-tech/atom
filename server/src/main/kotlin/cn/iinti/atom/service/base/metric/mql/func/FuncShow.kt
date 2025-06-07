package cn.iinti.atom.service.base.metric.mql.func

import cn.iinti.atom.service.base.metric.mql.Context
import cn.iinti.atom.service.base.metric.mql.Context.MQLVar
import cn.iinti.atom.service.base.metric.mql.func.MQLFunction.MQL_FUNC
import com.google.common.collect.Maps


@MQL_FUNC("show")
class FuncShow(params: List<String>) : MQLFunction(params) {
    override fun call(context: Context): MQLVar {
        for (`var` in params) {
            val line = context.variables[`var`]
            checkNotNull(line) { "no var: $`var`" }
            context.exportLines[`var`] = line
        }
        return MQLVar.newVar(Maps.newTreeMap())
    }
}
