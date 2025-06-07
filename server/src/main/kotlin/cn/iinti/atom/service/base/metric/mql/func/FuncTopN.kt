package cn.iinti.atom.service.base.metric.mql.func

import cn.iinti.atom.service.base.metric.EChart4MQL.Companion.legendId
import cn.iinti.atom.service.base.metric.MetricVo
import cn.iinti.atom.service.base.metric.mql.Context
import cn.iinti.atom.service.base.metric.mql.Context.MQLVar
import cn.iinti.atom.service.base.metric.mql.func.MQLFunction.MQL_FUNC
import com.google.common.collect.Maps
import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.math.NumberUtils
import java.util.*
import java.util.function.Consumer
import java.util.stream.Collectors


@MQL_FUNC("topN")
class FuncTopN(params: List<String>) : MQLFunction(params) {
    private val varName: String
    private var n: Int? = null
    private val revers: Boolean

    init {
        check(params.size >= 1) { "must has one param" }
        varName = params.get(0)
        if (params.size >= 2) {
            n = NumberUtils.toInt(params.get(1), 10)
        } else {
            n = 10
        }

        revers = params.size >= 3 && BooleanUtils.toBoolean(params.get(2))
    }


    override fun call(context: Context): MQLVar? {
        val `var`: MQLVar? = context.variables.get(varName)
        checkNotNull(`var`) { "no var : $varName" }

        val legendIdValues: MutableMap<String, Double> = Maps.newHashMap()

        `var`.data!!.values.forEach(Consumer { metricVos: List<MetricVo> ->
            metricVos.forEach(
                Consumer { metricVo: MetricVo ->
                    val key: String = legendId(varName, metricVo, true)
                    val addValue: Double = legendIdValues.computeIfAbsent(key) { it: String? -> 0.0 } + metricVo.value!!
                    legendIdValues[key] = addValue
                })
        })

        val keepLines: Set<String> = legendIdValues.entries.stream()
            .sorted { o1: Map.Entry<String, Double>, o2: Map.Entry<String?, Double?> ->
                if (revers) o1.value.compareTo(o2.value!!) else o2.value!!.compareTo(o1.value)
            }
            .limit(n!!.toLong())
            .map { it.key }.collect(Collectors.toSet())


        val newData: TreeMap<String, MutableList<MetricVo>> = TreeMap()

        `var`.data!!.forEach { (s: String, metricVos: MutableList<MetricVo>) ->
            newData[s] = metricVos.stream()
                .filter { metricVo: MetricVo? -> keepLines.contains(legendId(varName, metricVo!!, true)) }
                .toList()
                .toMutableList()
        }
        return MQLVar.newVar(newData)
    }
}
