package cn.iinti.atom.service.base.metric.mql.func

import cn.iinti.atom.service.base.metric.MetricVo
import cn.iinti.atom.service.base.metric.mql.Context
import cn.iinti.atom.service.base.metric.mql.Context.MQLVar
import cn.iinti.atom.service.base.metric.mql.func.MQLFunction.MQL_FUNC
import com.google.common.collect.Sets
import java.util.function.Consumer
import java.util.function.Function
import java.util.stream.Collectors
import java.util.stream.Stream


@MQL_FUNC("dropLeft")
class FuncDropLeft(params: List<String>) : MQLFunction(params) {
    private var count = 0
    private val `var`: String

    init {
        check(!params.isEmpty()) { "shift must has one param" }
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

        val allMetricByTagId = mqlVar.data!!.values.stream()
            .flatMap(Function { metricVos: List<MetricVo> ->
                metricVos.stream().map { obj: MetricVo -> obj.toTagId() }
            } as Function<List<MetricVo>, Stream<String>>)
            .collect(Collectors.toSet())

        val emptyTimeKeys: MutableSet<String> = Sets.newHashSet()

        allMetricByTagId.forEach(Consumer<String> { metricTagId: String ->
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
        })
        emptyTimeKeys.forEach(Consumer { s: String? -> mqlVar.data!!.remove(s!!) })
        return mqlVar
    }
}
