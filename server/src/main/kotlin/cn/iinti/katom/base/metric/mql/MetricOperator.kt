package cn.iinti.katom.base.metric.mql

import cn.iinti.katom.base.metric.MetricVo
import cn.iinti.katom.base.metric.MetricVo.Companion.cloneMetricVo
import cn.iinti.katom.base.metric.mql.Context.MQLVar
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.google.common.collect.Sets
import java.util.*
import java.util.function.BinaryOperator

import java.util.function.Function


/**
 * 实现指标的加减乘除四则运算
 * add/sub/divide/multiply
 */
class MetricOperator(
    private val leftParam: Function<Context, Any?>,
    private val rightParam: Function<Context, Any?>,
    private val operator: BinaryOperator<Double>
) : Function<Context, Any?> {
    override fun apply(context: Context): Any {
        val left: Any = checkParam(leftParam.apply(context))
        val right: Any = checkParam(rightParam.apply(context))

        if (left is Number && right is Number) {
            return doCalculate(left.toDouble(), right.toDouble())!!
        }

        if (left is MQLVar && right is MQLVar) {
            return calcMQLVar(left, right)
        }

        return calcMQLVarAndNumber(left, right)
    }


    private fun calcMQLVarAndNumber(left: Any, right: Any): MQLVar {
        val leftNum: Boolean
        val doubleValue: Double
        val mqlVar: MQLVar

        if (left is Number) {
            leftNum = true
            doubleValue = left.toDouble()
            mqlVar = right as MQLVar
        } else {
            leftNum = false
            doubleValue = (right as Number).toDouble()
            mqlVar = left as MQLVar
        }

        val data: TreeMap<String, MutableList<MetricVo>> = Maps.newTreeMap()
        mqlVar.data!!.forEach { (s: String, metricVos: List<MetricVo>) ->
            data[s] = metricVos
                .mapNotNull { metricVo: MetricVo ->
                    val ret: Double? = doCalculate(
                        if (leftNum) doubleValue else metricVo.value,
                        if (leftNum) metricVo.value else doubleValue
                    )
                    if (ret == null || ret.isNaN()) {
                        return@mapNotNull null
                    }
                    val retMetricVo: MetricVo = cloneMetricVo(metricVo)
                    retMetricVo.value = ret
                    retMetricVo
                }
                .toMutableList()
        }
        return MQLVar.newVar(data)
    }

    private fun calcMQLVar(left: MQLVar, right: MQLVar): MQLVar {
        val leftData: TreeMap<String, MutableList<MetricVo>>? = left.data
        val rightData: TreeMap<String, MutableList<MetricVo>>? = right.data

        val timeKeys: TreeSet<String> = Sets.newTreeSet()
        timeKeys.addAll(leftData!!.keys)
        timeKeys.addAll(rightData!!.keys)

        val ret: TreeMap<String, MutableList<MetricVo>> = Maps.newTreeMap()

        timeKeys.forEach { timeKey: String ->
            val leftMetricVos: MutableList<MetricVo> = leftData[timeKey] ?: mutableListOf()
            val rightMetricVos: MutableList<MetricVo> = rightData[timeKey] ?: mutableListOf()

            val leftByGroup = leftMetricVos.associateBy(
                keySelector = { obj -> obj.toTagId() },
                valueTransform = { i -> i }
            )

            val rightByGroup = rightMetricVos.associateBy(
                keySelector = { obj -> obj.toTagId() },
                valueTransform = { i -> i }
            )

            val calcPoint: MutableList<MetricVo> = Lists.newArrayList()
            ret[timeKey] = calcPoint

            val union: HashSet<String> = HashSet(leftByGroup.keys)
            union.retainAll(rightByGroup.keys)

            union.forEach { unionTag: String ->
                val leftMetricVo: MetricVo? = leftByGroup.get(unionTag)
                val rightMetricVo: MetricVo? = rightByGroup.get(unionTag)

                val metricVo: MetricVo = cloneMetricVo(leftMetricVo!!)
                metricVo.value = doCalculate(leftMetricVo.value, rightMetricVo!!.value)
                calcPoint.add(metricVo)

                leftMetricVos.remove(leftMetricVo)
                rightMetricVos.remove(rightMetricVo)
            }

            for (leftRemain: MetricVo? in leftMetricVos) {
                val metricVo: MetricVo = cloneMetricVo(leftRemain!!)
                metricVo.value = doCalculate(leftRemain.value, 0.0)
                calcPoint.add(metricVo)
            }
            for (rightRemain: MetricVo? in rightMetricVos) {
                val metricVo: MetricVo = cloneMetricVo(rightRemain!!)
                metricVo.value = doCalculate(0.0, rightRemain.value)
                calcPoint.add(metricVo)
            }
        }

        return MQLVar.newVar(ret)
    }

    private fun doCalculate(left: Double?, right: Double?): Double? {
        if (left == null || right == null) {
            return null
        }
        return operator.apply(left, right)
    }

    private fun checkParam(param: Any?): Any {
        checkNotNull(param) { "empty param" }
        check(!(param !is Number && param !is MQLVar)) { "AlgorithmUnit param must be number of TreeMap<String, List<MetricVo>> " }
        return param
    }

    companion object {
        fun add(leftParam: Function<Context, Any?>, rightParam: Function<Context, Any?>): MetricOperator {
            return MetricOperator(
                leftParam, rightParam,
                { a: Double, b: Double -> java.lang.Double.sum(a, b) })
        }

        fun minus(leftParam: Function<Context, Any?>, rightParam: Function<Context, Any?>): MetricOperator {
            return MetricOperator(
                leftParam, rightParam,
                { a: Double, b: Double -> a - b })
        }

        fun multiply(leftParam: Function<Context, Any?>, rightParam: Function<Context, Any?>): MetricOperator {
            return MetricOperator(
                leftParam, rightParam,
                { a: Double, b: Double -> a * b })
        }

        fun divide(leftParam: Function<Context, Any?>, rightParam: Function<Context, Any?>): MetricOperator {
            return MetricOperator(leftParam, rightParam, BinaryOperator { a: Double, b: Double ->
                if (a.compareTo(0.0) == 0) {
                    return@BinaryOperator 0.0
                }
                if (b.isNaN() || b.isInfinite() || b.compareTo(0.0) == 0) {
                    return@BinaryOperator 0.0
                }
                a / b
            })
        }
    }
}
