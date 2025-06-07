package cn.iinti.atom.service.base.metric.mql.func

import cn.iinti.atom.service.base.metric.MetricVo
import cn.iinti.atom.service.base.metric.mql.Context
import cn.iinti.atom.service.base.metric.mql.Context.MQLVar
import com.google.common.collect.Maps


abstract class XofVarFunc(params: List<String>) : MQLFunction(params) {
    private val varName: String

    init {
        check(params.isNotEmpty()) { "must has one param" }
        varName = params[0]
    }

    override fun call(context: Context): MQLVar {
        val `var` = context.variables[varName]
        checkNotNull(`var`) { "no var : $varName" }

        val ret = Maps.newTreeMap<String, MutableList<MetricVo>>()
        `var`.data!!.forEach { (s: String?, metricVos: MutableList<MetricVo>) ->
            ret[s] = apply(metricVos)
        }
        return MQLVar.newVar(ret)
    }

    protected abstract fun apply(metricVos: List<MetricVo>): MutableList<MetricVo>
}
