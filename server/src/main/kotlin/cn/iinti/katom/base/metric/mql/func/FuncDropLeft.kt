package cn.iinti.katom.base.metric.mql.func

import cn.iinti.katom.base.metric.mql.Context
import cn.iinti.katom.base.metric.mql.Context.MQLVar
import cn.iinti.katom.base.metric.mql.func.MQLFunction.MQL_FUNC
import com.google.common.collect.Sets


@MQL_FUNC("dropLeft")
class FuncDropLeft(params: List<String>) : MQLFunction(params) {
    private var count = 0
    private val `var`: String

    init {
        check(params.isNotEmpty()) { "shift must has one param" }
        `var` = params[0]
        count = if (params.size == 1) {
            1
        } else {
            params[1].toInt()
        }
    }

    private fun requireVar(context: Context): MQLVar {
        val mqlVar = context.variables[`var`]
        checkNotNull(mqlVar) { "no var : $`var`" }
        return mqlVar.copy()
    }

    override fun call(context: Context): MQLVar {
        val mqlVar = requireVar(context)

        val allMetricByTagId = mqlVar.data!!.values
            .flatMap { metricVos ->
                metricVos.map { metricVo ->
                    metricVo.toTagId()
                }
            }.toSet()

        val emptyTimeKeys: MutableSet<String> = Sets.newHashSet()

        allMetricByTagId.forEach { metricTagId: String ->
            var findCount = 0
            for ((key, metricVos) in mqlVar.data!!) {
                for (i in metricVos.indices) {
                    val metricVo = metricVos[i]
                    if (metricVo.toTagId() == metricTagId) {
                        metricVos.remove(metricVo)
                        findCount++
                        break
                    }
                }
                if (metricVos.isEmpty()) {
                    emptyTimeKeys.add(key)
                }
                if (findCount >= count) {
                    break
                }
            }
        }
        emptyTimeKeys.forEach { s: String? -> mqlVar.data!!.remove(s!!) }
        return mqlVar
    }
}
