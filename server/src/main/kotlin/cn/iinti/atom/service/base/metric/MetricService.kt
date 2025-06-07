package cn.iinti.atom.service.base.metric

import cn.iinti.atom.entity.metric.Metric
import cn.iinti.atom.entity.metric.MetricDay
import cn.iinti.atom.entity.metric.MetricTag
import cn.iinti.atom.mapper.metric.MetricDayMapper
import cn.iinti.atom.mapper.metric.MetricHourMapper
import cn.iinti.atom.mapper.metric.MetricMinuteMapper
import cn.iinti.atom.service.base.env.Environment
import cn.iinti.atom.service.base.env.Environment.Companion.registerShutdownHook
import cn.iinti.atom.service.base.perm.PermsService
import cn.iinti.atom.service.base.safethread.Looper
import cn.iinti.atom.system.AppContext.getUser
import com.alibaba.fastjson.JSONObject
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import io.micrometer.core.instrument.*
import jakarta.annotation.PostConstruct
import jakarta.annotation.Resource
import lombok.extern.slf4j.Slf4j
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.BeanUtils
import org.springframework.dao.DuplicateKeyException
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.function.Function
import java.util.stream.Collectors
import kotlin.math.max


@Service
class MetricService {
    private val metricHandleLooper = Looper("metric_handler").startLoop()
    private val log: Logger = LoggerFactory.getLogger(MetricService::class.java)

    @Resource
    private val metricDayMapper: MetricDayMapper? = null

    @Resource
    private val metricHourMapper: MetricHourMapper? = null

    @Resource
    private val metricMinuteMapper: MetricMinuteMapper? = null

    @Resource
    private val metricTagService: MetricTagService? = null

    @Resource
    private val permsService: PermsService? = null

    @PostConstruct
    fun registerShutdownHook() {
        registerShutdownHook { this.publishMetrics() }
    }

    @Scheduled(cron = "5 * * * * ?")
    fun publishMetrics() {
        if (Environment.isLocalDebug) {
            return
        }
        log.info(
            "schedule generator metric data for timeKey: {}",
            LocalDateTime.now().format(MetricEnums.MetricAccuracy.minutes.timePattern)
        )
        val now = LocalDateTime.now()
        val timeKey = now.format(MetricEnums.MetricAccuracy.minutes.timePattern)
        Metrics.globalRegistry.forEachMeter { meter: Meter ->
            try {
                onMeter(timeKey, now, meter)
            } catch (e: Exception) {
                log.error("error", e)
            }
        }
    }

    private val minuteCrt = MergeScanStartCtr(
        8 * 60, 2 * 60
    )

    @Scheduled(cron = "15 0/20 * * * ?")
    fun scheduleMergeMinuteToHours() {
        if (Environment.isLocalDebug) {
            return
        }
        log.info("schedule mergeHoursMetric")
        metricHandleLooper.execute {
            performMerge(
                MetricEnums.MetricAccuracy.minutes,
                MetricEnums.MetricAccuracy.hours,
                LocalDateTime.now().minusHours(36),
                minuteCrt.scanStart(),
                { localDateTime: LocalDateTime -> localDateTime.plusHours(1) },
                { localDateTime: LocalDateTime? -> localDateTime!!.withMinute(0).withSecond(0) }
            )
        }
    }

    private val hourCrt = MergeScanStartCtr(
        8 * 24 * 60, 2 * 24 * 60
    )

    @Scheduled(cron = "40 10 0/5 * * ?")
    fun scheduleMergeHourToDays() {
        if (Environment.isLocalDebug) {
            return
        }
        log.info("schedule mergeHourToDays")
        metricHandleLooper.execute {
            performMerge(
                MetricEnums.MetricAccuracy.hours,
                MetricEnums.MetricAccuracy.days,
                LocalDateTime.now().minusDays(40),
                hourCrt.scanStart(),
                { localDateTime: LocalDateTime -> localDateTime.plusDays(1) },
                { localDateTime: LocalDateTime? -> localDateTime!!.withHour(0).withMinute(0).withSecond(0) }
            )
        }
    }

    @Scheduled(cron = "0 4 4 14 * ?")
    fun scheduleCleanDays() {
        // 最多保留3年的指标数据，超过3年的直接删除
        chooseDao<Metric>(MetricEnums.MetricAccuracy.days).delete(
            QueryWrapper<Metric>()
                .le(Metric.CREATE_TIME, LocalDateTime.now().minusDays(1000))
        )
    }

    /**
     * 获取指标数据，根据key查询
     *
     * @param name     指标名称
     * @param accuracy 精度，包括分钟、小时、天，三个维度
     * @return 指标集合，请注意这里不会对数据做过滤。同时因为我们对指标有提前聚合，所以返回结果集是可控的，大约在千以内
     */
    fun queryMetric(name: String?, query: Map<String, String>, accuracy: MetricEnums.MetricAccuracy): List<MetricVo> {
        val metricTag = metricTagService!!.fromKey(name) ?: return emptyList()
        val queryWrapper = metricTagService
            .wrapQueryWithTags<Metric>(
                QueryWrapper<Metric>().eq(Metric.NAME, name),
                query, metricTag
            )

        if (getUser() != null) {
            permsService!!.filter(Metric::class.java, queryWrapper)
        }


        var metricRet = chooseDao<Metric>(accuracy).selectList(queryWrapper.orderByAsc(Metric.TIME_KEY))
            .stream()
            .map<MetricVo> { metric: Metric -> mapMetric(metric, metricTag) }

        metricRet = metricRet.peek { metricVo: MetricVo ->
            // remove tag field after
            val tags = metricVo.tags
            query.keys.forEach(Consumer { o: String -> tags.remove(o) })
        }

        return metricRet.collect(Collectors.toList())
    }


    private fun mapMetric(metric: Metric, metricTag: MetricTag): MetricVo {
        val metricVo = MetricVo()
        val tags = metricVo.tags
        BeanUtils.copyProperties(metric, metricVo)
        if (StringUtils.isNotBlank(metricTag.tag1Name)) {
            tags[metricTag.tag1Name] = metric.tag1
        }
        if (StringUtils.isNotBlank(metricTag.tag2Name)) {
            tags[metricTag.tag2Name] = metric.tag2
        }
        if (StringUtils.isNotBlank(metricTag.tag3Name)) {
            tags[metricTag.tag3Name] = metric.tag3
        }
        if (StringUtils.isNotBlank(metricTag.tag4Name)) {
            tags[metricTag.tag4Name] = metric.tag4
        }
        if (StringUtils.isNotBlank(metricTag.tag5Name)) {
            tags[metricTag.tag5Name] = metric.tag5
        }
        return metricVo
    }


    private fun performMerge(
        fromAccuracy: MetricEnums.MetricAccuracy,
        toAccuracy: MetricEnums.MetricAccuracy,
        cleanBefore: LocalDateTime, scanStart: LocalDateTime?,
        stepFun: Function<LocalDateTime, LocalDateTime>,
        stepStartFun: Function<LocalDateTime?, LocalDateTime>
    ) {
        metricTagService!!.metricNames().forEach({ metricName: String ->
            var scanStart_: LocalDateTime?
            if (scanStart == null) {
                val first = chooseDao<Metric>(fromAccuracy).selectOne(
                    QueryWrapper<Metric>()
                        .eq(Metric.NAME, metricName)
                        .orderByAsc(Metric.TIME_KEY)
                        .last("limit 1")
                )
                if (first == null) {
                    return@forEach
                }
                scanStart_ = first.createTime
            } else {
                scanStart_ = scanStart
            }
            val now = LocalDateTime.now()
            while (scanStart_!!.isBefore(now)) {
                val start = stepStartFun.apply(scanStart_)
                val end = stepFun.apply(start)
                mergeTimesSpace(start, end, fromAccuracy, toAccuracy, metricName, cleanBefore)
                scanStart_ = end.plusMinutes(30)
            }
            // clean metric data which has been merged and produced for a long time
            chooseDao<Metric>(fromAccuracy).delete(
                QueryWrapper<Metric>()
                    .eq(Metric.NAME, metricName)
                    .lt(Metric.CREATE_TIME, cleanBefore)
            )
        })
    }


    private fun mergeTimesSpace(
        startTime: LocalDateTime, endTime: LocalDateTime,
        from: MetricEnums.MetricAccuracy, to: MetricEnums.MetricAccuracy,
        metricName: String, cleanBefore: LocalDateTime
    ) {
        val timeKey = to.timePattern.format(startTime)
        log.info(
            "do merge metric from:{} ,to:{} timeKey:{} ,from accuracy:{} ,to accuracy:{} ",
            startTime.format(from.timePattern), endTime.format(from.timePattern),
            timeKey, from, to
        )

        // 一个指标，可能存在很多tag，tag分维如果有上千之后，再加上时间维度，数据量级就可能非常大，所以我们在数据库中先根据tag分组一下
        // 这样驻留在内存中的数据就没有tag维度，避免内存中数据量过大
        val tags = chooseDao<Metric>(from).selectObjs<Any>(
            QueryWrapper<Metric>()
                .select(Metric.TAGS_MD5)
                .eq(Metric.NAME, metricName)
                .ge(Metric.CREATE_TIME, startTime)
                .lt(Metric.CREATE_TIME, endTime)
                .groupBy(Metric.TAGS_MD5)
        ).stream().map { o: Any? -> o as String? }.toList()

        if (tags.isEmpty()) {
            return
        }

        tags.forEach(Consumer { tagsMd5: String? ->
            mergeTimesSpaceWithTag(
                startTime, endTime, from, to, metricName, tagsMd5, timeKey, cleanBefore
            )
        })
    }

    private fun mergeTimesSpaceWithTag(
        startTime: LocalDateTime, endTime: LocalDateTime,
        from: MetricEnums.MetricAccuracy, to: MetricEnums.MetricAccuracy,
        metricName: String, tagsMd5: String?, timeKey: String, cleanBefore: LocalDateTime
    ) {
        val metrics = chooseDao<Metric>(from).selectList(
            QueryWrapper<Metric>()
                .eq(Metric.NAME, metricName)
                .eq(Metric.TAGS_MD5, tagsMd5)
                .ge(Metric.CREATE_TIME, startTime)
                .lt(Metric.CREATE_TIME, endTime)
                .orderByAsc(Metric.TIME_KEY)
        )

        if (metrics.isEmpty()) {
            return
        }
        val first = metrics[0]

        val mergedList: MutableList<Metric> = Lists.newArrayList()

        if (first.type == Meter.Type.TIMER) {
            metrics.sortedWith { o1, o2 -> o1.timeKey!!.compareTo(o2.timeKey!!) }

            // for timer , need group by
            val countList: MutableList<Metric> = Lists.newArrayList()
            val totalTimeList: MutableList<Metric> = Lists.newArrayList()
            val maxList: MutableList<Metric> = Lists.newArrayList()
            val metricTag = metricTagService!!.fromKey(metricName)!!
            metrics.forEach(Consumer { metric: Metric ->
                val subtype = mapMetric(metric, metricTag).tags[MetricEnums.TimeSubType.timer_type]
                if (MetricEnums.TimeSubType.COUNT.metricKey == subtype) {
                    countList.add(metric)
                } else if (MetricEnums.TimeSubType.TIME.metricKey == subtype) {
                    totalTimeList.add(metric)
                } else if (MetricEnums.TimeSubType.MAX.metricKey == subtype) {
                    maxList.add(metric)
                }
            })
            if (!countList.isEmpty()) {
                mergedList.add(mergeMetrics(countList, countList[0], SUM))
            }
            if (!totalTimeList.isEmpty()) {
                mergedList.add(mergeMetrics(totalTimeList, totalTimeList[0], SUM))
            }
            if (!maxList.isEmpty()) {
                mergedList.add(mergeMetrics(maxList, maxList[0], MAX))
            }
        } else {
            mergedList.add(mergeMetrics(metrics, first, if (first.type == Meter.Type.COUNTER) SUM else AVG))
        }

        mergedList.forEach(Consumer<Metric> { mergedMetric: Metric ->
            mergedMetric.timeKey = timeKey
            mergedMetric.createTime = first.createTime
            log.info("merged metric: {}", JSONObject.toJSONString(mergedMetric))
            try {
                chooseDao<Metric>(to).insert(mergedMetric)
            } catch (ignore: DuplicateKeyException) {
                // update on duplicate
                // for newer data, I will merge many times
                val old = chooseDao<Metric>(to).selectOne(
                    QueryWrapper<Metric>()
                        .eq(Metric.NAME, mergedMetric.name)
                        .eq(Metric.TAGS_MD5, mergedMetric.tagsMd5)
                        .eq(Metric.TIME_KEY, mergedMetric.timeKey)
                )
                if (old != null) {
                    mergedMetric.id = old.id
                    chooseDao<Metric>(to).updateById(mergedMetric)
                }
            }
        })

        val needRemoveIds = metrics.stream()
            .filter { metric: Metric -> metric.createTime!!.isBefore(cleanBefore) }
            .map(Metric::id)
            .collect(Collectors.toList())

        if (!needRemoveIds.isEmpty()) {
            chooseDao<Metric>(from).deleteBatchIds(needRemoveIds)
        }
    }

    private fun mergeMetrics(
        metrics: List<Metric>, first: Metric, mergeFunc: (List<Metric>) -> Double
    ): Metric {
        val container = Metric()
        BeanUtils.copyProperties(first, container)
        container.value = mergeFunc(metrics)
        return container
    }

    /**/////  metric producer start   ///////// */
    fun onMeter(timeKey: String, time: LocalDateTime, meter: Meter) {
        val type = meter.id.type

        if (type == Meter.Type.GAUGE) {
            val metricTag = metricTagService!!.fromMeter(meter, null)
            val metric = makeMetric(timeKey, time, meter, metricTag, null)
            saveGauge(metric, meter as Gauge)
        } else if (type == Meter.Type.COUNTER) {
            val metricTag = metricTagService!!.fromMeter(meter, null)
            val metric = makeMetric(timeKey, time, meter, metricTag, null)
            val count = if (meter is Counter) {
                meter.count()
            } else if (meter is FunctionCounter) {
                meter.count()
            } else {
                log.warn("unknown counter type:{}", meter.javaClass)
                return
            }
            saveCounter(metric, metric.tagsMd5, count)
        } else if (type == Meter.Type.TIMER) {
            val timer = meter as Timer
            // 三个分量，分别用三个指标存储
            val count = timer.count()
            val totalTime = timer.totalTime(TimeUnit.MILLISECONDS)
            val max = timer.max(TimeUnit.MILLISECONDS)

            val metricTagTime = metricTagService!!.fromMeter(meter, MetricEnums.TimeSubType.TIME)
            val metricTime = makeMetric(timeKey, time, meter, metricTagTime, MetricEnums.TimeSubType.TIME)
            saveCounter(metricTime, metricTime.tagsMd5, totalTime)

            val metricTagCount = metricTagService.fromMeter(meter, MetricEnums.TimeSubType.COUNT)
            val metricCount = makeMetric(timeKey, time, meter, metricTagCount, MetricEnums.TimeSubType.COUNT)
            saveCounter(metricCount, metricCount.tagsMd5, count.toDouble())

            val metricTagMax = metricTagService.fromMeter(meter, MetricEnums.TimeSubType.MAX)
            val metricMax = makeMetric(timeKey, time, meter, metricTagMax, MetricEnums.TimeSubType.MAX)
            metricMax.value = max
            saveMetric(metricMax)
        }
    }


    private fun saveGauge(metric: Metric, gauge: Gauge) {
        var value = gauge.value()
        if (java.lang.Double.isNaN(value)) {
            // 暂时还不确认为啥有NaN出现
            log.error("error gauge for metric: {}", JSONObject.toJSONString(metric))
            value = 0.0
        }
        metric.value = value
        saveMetric(metric)
    }

    private val lastValues: MutableMap<String?, Double> = Maps.newConcurrentMap()

    private fun saveCounter(metric: Metric, uniKey: String?, nowCount: Double) {
        var history = lastValues[uniKey]
        if (history == null) {
            history = 0.0
        }

        var add = nowCount - history
        if (add < 0) {
            // maybe reset??
            add = 0.0
        }
        metric.value = add
        lastValues[uniKey] = nowCount
        saveMetric(metric)
    }

    private fun saveMetric(metric: Metric) {
        try {
            metric.createTime = LocalDateTime.now()
            chooseDao<Metric>(MetricEnums.MetricAccuracy.minutes).insert(metric)
        } catch (ignore: DuplicateKeyException) {
        }
    }

    private fun makeMetric(
        timeKey: String,
        time: LocalDateTime,
        meter: Meter,
        metricTag: MetricTag,
        timerType: MetricEnums.TimeSubType?
    ): Metric {
        val id = meter.id
        val metric = Metric()
        metric.name = id.name
        metric.createTime = time

        metricTagService!!.setupTag(metricTag, meter, metric, timerType)
        metric.type = meter.id.type
        metric.timeKey = timeKey
        return metric
    }

    /**/////  metric producer end   ///////// */
    private class MergeScanStartCtr(private val fullDurationMinute: Int, private val scanStartBeforeMinute: Int) {
        private var lastFull: LocalDateTime? = null

        fun scanStart(): LocalDateTime? {
            if (lastFull == null) {
                lastFull = LocalDateTime.now()
                return null
            }
            val now = LocalDateTime.now()
            if (lastFull!!.plusMinutes(fullDurationMinute.toLong()).isAfter(now)) {
                return now.minusMinutes(scanStartBeforeMinute.toLong())
            }
            lastFull = now
            return null
        }
    }

    fun <T : Metric> chooseDao(accuracy: MetricEnums.MetricAccuracy): BaseMapper<T> {
        return when (accuracy) {
            MetricEnums.MetricAccuracy.days -> (metricDayMapper as BaseMapper<T>)
            MetricEnums.MetricAccuracy.hours -> (metricHourMapper as BaseMapper<T>)
            else -> (metricMinuteMapper as BaseMapper<T>)
        }
    }

    fun eachDao(consumer: Consumer<BaseMapper<Metric>>) {
        consumer.accept(chooseDao(MetricEnums.MetricAccuracy.days))
        consumer.accept(chooseDao(MetricEnums.MetricAccuracy.hours))
        consumer.accept(chooseDao(MetricEnums.MetricAccuracy.minutes))
    }

    companion object {
        private val AVG = { metrics: List<Metric> ->
            metrics.stream()
                .map(Metric::value)
                .reduce(0.0) { a, b ->
                    (a!! + b!!) / metrics.size
                }!!
        }

        private val SUM = { metrics: List<Metric> ->
            metrics.stream()
                .map(Metric::value)
                .reduce(0.0) { a, b -> a!! + b!! }!!
        }

        private val MAX = { metrics: List<Metric> ->
            metrics.stream()
                .map(Metric::value)
                .reduce(0.0) { a, b -> max(a!!, b!!) }!!
        }
    }
}
