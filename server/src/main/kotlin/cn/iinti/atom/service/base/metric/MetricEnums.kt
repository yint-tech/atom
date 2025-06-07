package cn.iinti.atom.service.base.metric

import proguard.annotation.Keep
import java.time.format.DateTimeFormatter


class MetricEnums {
    @Keep
    enum class MetricAccuracy(val timePattern: DateTimeFormatter) {
        minutes(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH:mm")),
        hours(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH")),
        days(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    }

    @Keep
    enum class TimeSubType(val metricKey: String) {
        TIME("time"),
        COUNT("count"),
        MAX("max");


        companion object {
            const val timer_type: String = "timer_type"
        }
    }
}
