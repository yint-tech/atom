package cn.iinti.atom.service.base.metric

import proguard.annotation.Keep
import java.time.format.DateTimeFormatter


class MetricEnums {
    @Keep
    enum class MetricAccuracy(val timePattern: DateTimeFormatter) {
        MINUTES(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH:mm")),
        HOURS(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH")),
        DAYS(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    }

    @Keep
    enum class TimeSubType(val metricKey: String) {
        TIME("time"),
        COUNT("count"),
        MAX("max");


        companion object {
            const val TIMER_TYPE: String = "timer_type"
        }
    }
}
