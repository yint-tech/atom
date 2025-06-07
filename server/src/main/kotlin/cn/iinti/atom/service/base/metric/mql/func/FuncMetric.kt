package cn.iinti.atom.service.base.metric.mql.func

import cn.iinti.atom.service.base.metric.MetricVo
import cn.iinti.atom.service.base.metric.mql.Context
import cn.iinti.atom.service.base.metric.mql.Context.MQLVar
import cn.iinti.atom.service.base.metric.mql.compile.MQLCompiler.BadGrammarException
import cn.iinti.atom.service.base.metric.mql.func.MQLFunction.MQL_FUNC
import com.google.common.collect.Maps
import java.util.*
import java.util.stream.Collectors


@MQL_FUNC("metric")
class FuncMetric(params: List<String>) : MQLFunction(params) {
    private val metricName: String
    private val filters: MutableMap<String, String> = Maps.newHashMap()


    init {
        check(params.isNotEmpty()) { "metric function need param" }
        metricName = params[0]
        var key: String? = null
        for (i in 1..<params.size) {
            val token = params[i]
            if (token == "[" || token == "]" || token == "=") {
                continue
            }
            if (key == null) {
                key = token
                continue
            }
            filters[key] = token
            key = null
        }

        if (key != null) {
            throw BadGrammarException("no filter value for key:$key")
        }
    }


    override fun call(context: Context): MQLVar {
        val metrics: List<MetricVo> =
            context.metricService.queryMetric(metricName, filters, context.metricAccuracy)


        val metricWithTime = metrics.stream().collect(Collectors.groupingBy(MetricVo::timeKey))
        val treeMap: TreeMap<String, MutableList<MetricVo>> = TreeMap()
        for ((key, value) in metricWithTime) {
            treeMap[key!!] = value
        }
        return MQLVar.newVar(treeMap)
    }
}
