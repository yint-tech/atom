package cn.iinti.katom.base.metric.mql.func

import cn.iinti.katom.base.metric.MetricEnums.MetricAccuracy
import cn.iinti.katom.base.metric.MetricVo
import cn.iinti.katom.base.metric.MetricVo.Companion.cloneMetricVo
import cn.iinti.katom.base.metric.mql.Context
import cn.iinti.katom.base.metric.mql.Context.MQLVar
import cn.iinti.katom.base.metric.mql.func.MQLFunction.MQL_FUNC
import com.google.common.collect.Maps
import java.time.LocalDateTime
import java.util.*
import java.util.function.Function


@MQL_FUNC("shift")
class FuncShift(params: List<String>) : MQLFunction(params) {
    private val shiftVal: String
    private val shiftFunc: MutableMap<MetricAccuracy, Function<LocalDateTime?, LocalDateTime>> = Maps.newHashMap()

    init {
        check(params.isNotEmpty()) { "shift must has one param" }
        shiftVal = params[0]
        parseParam(params)
    }


    private fun parseParam(params: List<String>) {
        var count = 1
        if (params.size > 1) {
            count = params.get(1).toInt()
        }
        if (params.size > 2) {
            val func: Function<LocalDateTime?, LocalDateTime> = definitionFun(count, params.get(2))
            shiftFunc[MetricAccuracy.MINUTES] = func
            shiftFunc[MetricAccuracy.HOURS] = func
            shiftFunc[MetricAccuracy.DAYS] = func
        } else {
            val finalCount: Int = count
            shiftFunc[MetricAccuracy.MINUTES] =
                Function { t: LocalDateTime? -> t!!.minusMinutes(finalCount.toLong()) }
            shiftFunc[MetricAccuracy.HOURS] =
                Function { t: LocalDateTime? -> t!!.minusHours(finalCount.toLong()) }
            shiftFunc[MetricAccuracy.DAYS] =
                Function { t: LocalDateTime? -> t!!.minusDays(finalCount.toLong()) }
        }
    }

    private fun definitionFun(count: Int, unit: String): Function<LocalDateTime?, LocalDateTime> {
        return when (unit.lowercase(Locale.getDefault())) {
            "minute" -> Function { t: LocalDateTime? -> t!!.minusMinutes(count.toLong()) }
            "hour" -> Function { t: LocalDateTime? -> t!!.minusHours(count.toLong()) }
            "day" -> Function { t: LocalDateTime? -> t!!.minusDays(count.toLong()) }
            "month" -> Function { t: LocalDateTime? -> t!!.minusMonths(count.toLong()) }
            else -> throw IllegalStateException("unknown shift unit")
        }
    }

    override fun call(context: Context): MQLVar {
        val mqlVar: MQLVar? = context.variables[shiftVal]
        checkNotNull(mqlVar) { "no var : $shiftVal" }
        val ret: TreeMap<String, MutableList<MetricVo>> = Maps.newTreeMap()
        mqlVar.data!!.forEach { (timeKey: String, metricVos: MutableList<MetricVo>) ->
            if (metricVos.isEmpty()) {
                return@forEach
            }
            val nowNode: MetricVo = metricVos.iterator().next()
            val shiftTime: LocalDateTime = shiftFunc[context.metricAccuracy]!!.apply(nowNode.createTime)
            val shiftTimeStr: String = context.metricAccuracy.timePattern.format(shiftTime)
            val shiftMetrics: MutableList<MetricVo> = mqlVar.data!!.get(shiftTimeStr) ?: return@forEach
            ret[timeKey] = shiftMetrics.map { metricVo: MetricVo? ->
                val ret1: MetricVo = cloneMetricVo(metricVo!!)
                ret1.createTime = nowNode.createTime
                ret1.timeKey = timeKey
                ret1
            }.toMutableList()
        }

        return MQLVar.newVar(ret)
    }
}
