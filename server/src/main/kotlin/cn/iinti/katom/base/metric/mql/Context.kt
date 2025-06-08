package cn.iinti.katom.base.metric.mql

import cn.iinti.katom.base.metric.MetricEnums.MetricAccuracy
import cn.iinti.katom.base.metric.MetricService
import cn.iinti.katom.base.metric.MetricVo
import com.google.common.collect.Maps
import lombok.Getter
import java.util.*


@Getter
class Context(val metricAccuracy: MetricAccuracy, val metricService: MetricService) {
    val variables: MutableMap<String, MQLVar> = Maps.newHashMap()

    val exportLines: MutableMap<String, MQLVar> = Maps.newHashMap()


    @Getter
    class MQLVar {
        /**
         * key is time
         */
        var data: TreeMap<String, MutableList<MetricVo>>? = null

        fun copy(): MQLVar {
            val newData = Maps.newTreeMap<String, MutableList<MetricVo>>()
            data!!.forEach { (s: String?, metricVos: List<MetricVo>) ->
                newData[s] = metricVos
                    .map { obj: MetricVo -> MetricVo.cloneMetricVo(obj) }
                    .toMutableList()
            }
            return newVar(newData)
        }

        companion object {
            fun newVar(data: TreeMap<String, MutableList<MetricVo>>): MQLVar {
                val mqlVar = MQLVar()
                mqlVar.data = data
                return mqlVar
            }
        }
    }
}