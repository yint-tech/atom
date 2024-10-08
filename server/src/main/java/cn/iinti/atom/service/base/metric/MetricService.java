package cn.iinti.atom.service.base.metric;

import cn.iinti.atom.entity.metric.Metric;
import cn.iinti.atom.entity.metric.MetricDay;
import cn.iinti.atom.entity.metric.MetricTag;
import cn.iinti.atom.mapper.metric.MetricDayMapper;
import cn.iinti.atom.mapper.metric.MetricHourMapper;
import cn.iinti.atom.mapper.metric.MetricMinuteMapper;
import cn.iinti.atom.service.base.env.Environment;
import cn.iinti.atom.service.base.perm.PermsService;
import cn.iinti.atom.service.base.safethread.Looper;
import cn.iinti.atom.system.AppContext;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.micrometer.core.instrument.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class MetricService {
    private final Looper metricHandleLooper = new Looper("metric_handler").startLoop();

    @Resource
    private MetricDayMapper metricDayMapper;

    @Resource
    private MetricHourMapper metricHourMapper;

    @Resource
    private MetricMinuteMapper metricMinuteMapper;

    @Resource
    private MetricTagService metricTagService;

    @Resource
    private PermsService permsService;

    @PostConstruct
    public void registerShutdownHook() {
        Environment.registerShutdownHook(this::publishMetrics);
    }

    @Scheduled(cron = "5 * * * * ?")
    public void publishMetrics() {
        if (Environment.isLocalDebug) {
            return;
        }
        log.info("schedule generator metric data for timeKey: {}",
                LocalDateTime.now().format(MetricEnums.MetricAccuracy.minutes.timePattern)
        );
        LocalDateTime now = LocalDateTime.now();
        String timeKey = now.format(MetricEnums.MetricAccuracy.minutes.timePattern);
        Metrics.globalRegistry.forEachMeter(meter -> {
            try {
                onMeter(timeKey, now, meter);
            } catch (Exception e) {
                log.error("error", e);
            }
        });
    }

    private final MergeScanStartCtr minuteCrt = new MergeScanStartCtr(
            8 * 60, 2 * 60
    );

    @Scheduled(cron = "15 0/20 * * * ?")
    public void scheduleMergeMinuteToHours() {
        if (Environment.isLocalDebug) {
            return;
        }
        log.info("schedule mergeHoursMetric");
        metricHandleLooper.execute(() ->
                performMerge(
                        MetricEnums.MetricAccuracy.minutes, MetricEnums.MetricAccuracy.hours,
                        LocalDateTime.now().minusHours(36), minuteCrt.scanStart(),
                        localDateTime -> localDateTime.plusHours(1),
                        localDateTime -> localDateTime.withMinute(0).withSecond(0)
                ));
    }

    private final MergeScanStartCtr hourCrt = new MergeScanStartCtr(
            8 * 24 * 60, 2 * 24 * 60
    );

    @Scheduled(cron = "40 10 0/5 * * ?")
    public void scheduleMergeHourToDays() {
        if (Environment.isLocalDebug) {
            return;
        }
        log.info("schedule mergeHourToDays");
        metricHandleLooper.execute(() ->
                performMerge(
                        MetricEnums.MetricAccuracy.hours, MetricEnums.MetricAccuracy.days,
                        LocalDateTime.now().minusDays(40), hourCrt.scanStart(),
                        localDateTime -> localDateTime.plusDays(1),
                        localDateTime -> localDateTime.withHour(0).withMinute(0).withSecond(0)
                ));
    }

    @Scheduled(cron = "0 4 4 14 * ?")
    public void scheduleCleanDays() {
        // 最多保留3年的指标数据，超过3年的直接删除
        chooseDao(MetricEnums.MetricAccuracy.days).delete(new QueryWrapper<Metric>()
                .le(Metric.CREATE_TIME, LocalDateTime.now().minusDays(1000))
        );
    }

    /**
     * 获取指标数据，根据key查询
     *
     * @param name     指标名称
     * @param accuracy 精度，包括分钟、小时、天，三个维度
     * @return 指标集合，请注意这里不会对数据做过滤。同时因为我们对指标有提前聚合，所以返回结果集是可控的，大约在千以内
     */
    public List<MetricVo> queryMetric(String name, Map<String, String> query, MetricEnums.MetricAccuracy accuracy) {
        MetricTag metricTag = metricTagService.fromKey(name);
        if (metricTag == null) {
            return Collections.emptyList();
        }
        QueryWrapper<Metric> queryWrapper = metricTagService
                .wrapQueryWithTags(new QueryWrapper<Metric>().eq(MetricDay.NAME, name),
                        query, metricTag);

        if (AppContext.getUser() != null) {
            permsService.filter(Metric.class, queryWrapper);
        }


        Stream<MetricVo> metricRet = chooseDao(accuracy).selectList(queryWrapper.orderByAsc(MetricDay.TIME_KEY))
                .stream()
                .map(metric -> mapMetric(metric, metricTag));

        metricRet = metricRet.peek(metricVo -> {
            // remove tag field after
            Map<String, String> tags = metricVo.getTags();
            query.keySet().forEach(tags::remove);
        });

        return metricRet.collect(Collectors.toList());
    }


    private MetricVo mapMetric(Metric metric, MetricTag metricTag) {
        MetricVo metricVo = new MetricVo();
        Map<String, String> tags = metricVo.getTags();
        BeanUtils.copyProperties(metric, metricVo);
        if (StringUtils.isNotBlank(metricTag.getTag1Name())) {
            tags.put(metricTag.getTag1Name(), metric.getTag1());
        }
        if (StringUtils.isNotBlank(metricTag.getTag2Name())) {
            tags.put(metricTag.getTag2Name(), metric.getTag2());
        }
        if (StringUtils.isNotBlank(metricTag.getTag3Name())) {
            tags.put(metricTag.getTag3Name(), metric.getTag3());
        }
        if (StringUtils.isNotBlank(metricTag.getTag4Name())) {
            tags.put(metricTag.getTag4Name(), metric.getTag4());
        }
        if (StringUtils.isNotBlank(metricTag.getTag5Name())) {
            tags.put(metricTag.getTag5Name(), metric.getTag5());
        }
        return metricVo;
    }


    private void performMerge(MetricEnums.MetricAccuracy fromAccuracy,
                              MetricEnums.MetricAccuracy toAccuracy,
                              LocalDateTime cleanBefore, LocalDateTime scanStart,
                              Function<LocalDateTime, LocalDateTime> stepFun,
                              Function<LocalDateTime, LocalDateTime> stepStartFun) {
        metricTagService.metricNames().forEach(metricName -> {
            LocalDateTime scanStart_;
            if (scanStart == null) {
                Metric first = chooseDao(fromAccuracy).selectOne(new QueryWrapper<Metric>()
                        .eq(Metric.NAME, metricName)
                        .orderByAsc(Metric.TIME_KEY)
                        .last("limit 1"));
                if (first == null) {
                    return;
                }
                scanStart_ = first.getCreateTime();
            } else {
                scanStart_ = scanStart;
            }
            LocalDateTime now = LocalDateTime.now();
            while (scanStart_.isBefore(now)) {
                LocalDateTime start = stepStartFun.apply(scanStart_);
                LocalDateTime end = stepFun.apply(start);
                mergeTimesSpace(start, end, fromAccuracy, toAccuracy, metricName, cleanBefore);
                scanStart_ = end.plusMinutes(30);
            }
            // clean metric data which has been merged and produced for a long time
            chooseDao(fromAccuracy).delete(new QueryWrapper<Metric>()
                    .eq(Metric.NAME, metricName)
                    .lt(Metric.CREATE_TIME, cleanBefore)
            );
        });
    }


    private void mergeTimesSpace(LocalDateTime startTime, LocalDateTime endTime,
                                 MetricEnums.MetricAccuracy from, MetricEnums.MetricAccuracy to,
                                 String metricName, LocalDateTime cleanBefore) {
        String timeKey = to.timePattern.format(startTime);
        log.info("do merge metric from:{} ,to:{} timeKey:{} ,from accuracy:{} ,to accuracy:{} ",
                startTime.format(from.timePattern), endTime.format(from.timePattern),
                timeKey, from, to);

        // 一个指标，可能存在很多tag，tag分维如果有上千之后，再加上时间维度，数据量级就可能非常大，所以我们在数据库中先根据tag分组一下
        // 这样驻留在内存中的数据就没有tag维度，避免内存中数据量过大
        List<String> tags = chooseDao(from).selectObjs(new QueryWrapper<Metric>()
                .select(Metric.TAGS_MD5)
                .eq(Metric.NAME, metricName)
                .ge(Metric.CREATE_TIME, startTime)
                .lt(Metric.CREATE_TIME, endTime)
                .groupBy(Metric.TAGS_MD5)
        ).stream().map(o -> (String) o).toList();

        if (tags.isEmpty()) {
            return;
        }

        tags.forEach(tagsMd5 -> mergeTimesSpaceWithTag(
                startTime, endTime, from, to, metricName, tagsMd5, timeKey, cleanBefore
        ));
    }

    private void mergeTimesSpaceWithTag(LocalDateTime startTime, LocalDateTime endTime,
                                        MetricEnums.MetricAccuracy from, MetricEnums.MetricAccuracy to,
                                        String metricName, String tagsMd5, String timeKey, LocalDateTime cleanBefore) {
        List<Metric> metrics = chooseDao(from).selectList(new QueryWrapper<Metric>()
                .eq(Metric.NAME, metricName)
                .eq(Metric.TAGS_MD5, tagsMd5)
                .ge(Metric.CREATE_TIME, startTime)
                .lt(Metric.CREATE_TIME, endTime)
                .orderByAsc(Metric.TIME_KEY)
        );

        if (metrics.isEmpty()) {
            return;
        }
        Metric first = metrics.get(0);

        List<Metric> mergedList = Lists.newArrayList();

        if (first.getType() == Meter.Type.TIMER) {
            metrics.sort(Comparator.comparing(Metric::getTimeKey));
            // for timer , need group by
            List<Metric> countList = Lists.newArrayList();
            List<Metric> totalTimeList = Lists.newArrayList();
            List<Metric> maxList = Lists.newArrayList();
            MetricTag metricTag = metricTagService.fromKey(metricName);
            metrics.forEach(metric -> {
                String subtype = mapMetric(metric, metricTag).getTags().get(MetricEnums.TimeSubType.timer_type);
                if (MetricEnums.TimeSubType.COUNT.metricKey.equals(subtype)) {
                    countList.add(metric);
                } else if (MetricEnums.TimeSubType.TIME.metricKey.equals(subtype)) {
                    totalTimeList.add(metric);
                } else if (MetricEnums.TimeSubType.MAX.metricKey.equals(subtype)) {
                    maxList.add(metric);
                }
            });
            if (!countList.isEmpty()) {
                mergedList.add(mergeMetrics(countList, countList.get(0), SUM));
            }
            if (!totalTimeList.isEmpty()) {
                mergedList.add(mergeMetrics(totalTimeList, totalTimeList.get(0), SUM));
            }
            if (!maxList.isEmpty()) {
                mergedList.add(mergeMetrics(maxList, maxList.get(0), MAX));
            }
        } else {
            mergedList.add(mergeMetrics(metrics, first, first.getType() == Meter.Type.COUNTER ? SUM : AVG));
        }

        mergedList.forEach(mergedMetric -> {
            mergedMetric.setTimeKey(timeKey);
            mergedMetric.setCreateTime(first.getCreateTime());
            log.info("merged metric: {}", JSONObject.toJSONString(mergedMetric));
            try {
                chooseDao(to).insert(mergedMetric);
            } catch (DuplicateKeyException ignore) {
                // update on duplicate
                // for newer data, I will merge many times
                Metric old = chooseDao(to).selectOne(new QueryWrapper<Metric>()
                        .eq(Metric.NAME, mergedMetric.getName())
                        .eq(Metric.TAGS_MD5, mergedMetric.getTagsMd5())
                        .eq(Metric.TIME_KEY, mergedMetric.getTimeKey())
                );
                if (old != null) {
                    mergedMetric.setId(old.getId());
                    chooseDao(to).updateById(mergedMetric);
                }
            }
        });

        List<Long> needRemoveIds = metrics.stream()
                .filter(metric -> metric.getCreateTime().isBefore(cleanBefore))
                .map(Metric::getId)
                .collect(Collectors.toList());

        if (!needRemoveIds.isEmpty()) {
            chooseDao(from).deleteBatchIds(needRemoveIds);
        }


    }

    private Metric mergeMetrics(List<Metric> metrics, Metric first, Function<List<Metric>, Double> mergeFunc) {
        Metric container = new Metric();
        BeanUtils.copyProperties(first, container);
        container.setValue(mergeFunc.apply(metrics));
        return container;
    }

    private static final Function<List<Metric>, Double> AVG = metrics ->
            metrics.stream()
                    .map(Metric::getValue)
                    .reduce(0D, Double::sum) / metrics.size();

    private static final Function<List<Metric>, Double> SUM = metrics ->
            metrics.stream()
                    .map(Metric::getValue)
                    .reduce(0D, Double::sum);

    private static final Function<List<Metric>, Double> MAX = metrics ->
            metrics.stream()
                    .map(Metric::getValue)
                    .reduce(0D, Double::max);


    ////////  metric producer start   /////////
    public void onMeter(String timeKey, LocalDateTime time, Meter meter) {
        Meter.Type type = meter.getId().getType();

        if (type == Meter.Type.GAUGE) {
            MetricTag metricTag = metricTagService.fromMeter(meter, null);
            Metric metric = makeMetric(timeKey, time, meter, metricTag, null);
            saveGauge(metric, (Gauge) meter);
        } else if (type == Meter.Type.COUNTER) {
            MetricTag metricTag = metricTagService.fromMeter(meter, null);
            Metric metric = makeMetric(timeKey, time, meter, metricTag, null);
            double count;
            if (meter instanceof Counter) {
                count = ((Counter) meter).count();
            } else if (meter instanceof FunctionCounter) {
                count = ((FunctionCounter) meter).count();
            } else {
                log.warn("unknown counter type:{}", meter.getClass());
                return;
            }
            saveCounter(metric, metric.getTagsMd5(), count);
        } else if (type == Meter.Type.TIMER) {
            Timer timer = (Timer) meter;
            // 三个分量，分别用三个指标存储
            long count = timer.count();
            double totalTime = timer.totalTime(TimeUnit.MILLISECONDS);
            double max = timer.max(TimeUnit.MILLISECONDS);

            MetricTag metricTagTime = metricTagService.fromMeter(meter, MetricEnums.TimeSubType.TIME);
            Metric metricTime = makeMetric(timeKey, time, meter, metricTagTime, MetricEnums.TimeSubType.TIME);
            saveCounter(metricTime, metricTime.getTagsMd5(), totalTime);

            MetricTag metricTagCount = metricTagService.fromMeter(meter, MetricEnums.TimeSubType.COUNT);
            Metric metricCount = makeMetric(timeKey, time, meter, metricTagCount, MetricEnums.TimeSubType.COUNT);
            saveCounter(metricCount, metricCount.getTagsMd5(), (double) count);

            MetricTag metricTagMax = metricTagService.fromMeter(meter, MetricEnums.TimeSubType.MAX);
            Metric metricMax = makeMetric(timeKey, time, meter, metricTagMax, MetricEnums.TimeSubType.MAX);
            metricMax.setValue(max);
            saveMetric(metricMax);
        }

    }


    private void saveGauge(Metric metric, Gauge gauge) {
        double value = gauge.value();
        if (Double.isNaN(value)) {
            // 暂时还不确认为啥有NaN出现
            log.error("error gauge for metric: {}", JSONObject.toJSONString(metric));
            value = 0.0;
        }
        metric.setValue(value);
        saveMetric(metric);
    }

    private final Map<String, Double> lastValues = Maps.newConcurrentMap();

    private void saveCounter(Metric metric, String uniKey, Double nowCount) {
        Double history = lastValues.get(uniKey);
        if (history == null) {
            history = 0D;
        }

        double add = nowCount - history;
        if (add < 0) {
            // maybe reset??
            add = 0;
        }
        metric.setValue(add);
        lastValues.put(uniKey, nowCount);
        saveMetric(metric);
    }

    private void saveMetric(Metric metric) {
        try {
            metric.setCreateTime(LocalDateTime.now());
            chooseDao(MetricEnums.MetricAccuracy.minutes).insert(metric);
        } catch (DuplicateKeyException ignore) {
        }
    }

    private Metric makeMetric(String timeKey, LocalDateTime time, Meter meter, MetricTag metricTag, MetricEnums.TimeSubType timerType) {
        Meter.Id id = meter.getId();
        Metric metric = new Metric();
        metric.setName(id.getName());
        metric.setCreateTime(time);

        metricTagService.setupTag(metricTag, meter, metric, timerType);
        metric.setType(meter.getId().getType());
        metric.setTimeKey(timeKey);
        return metric;
    }

    ////////  metric producer end   /////////

    private static class MergeScanStartCtr {
        private LocalDateTime lastFull;
        private final int fullDurationMinute;
        private final int scanStartBeforeMinute;

        public MergeScanStartCtr(int fullDurationMinute, int scanStartBeforeMinute) {
            this.fullDurationMinute = fullDurationMinute;
            this.scanStartBeforeMinute = scanStartBeforeMinute;
        }

        public LocalDateTime scanStart() {
            if (lastFull == null) {
                lastFull = LocalDateTime.now();
                return null;
            }
            LocalDateTime now = LocalDateTime.now();
            if (lastFull.plusMinutes(fullDurationMinute).isAfter(now)) {
                return now.minusMinutes(scanStartBeforeMinute);
            }
            lastFull = now;
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Metric> BaseMapper<T> chooseDao(MetricEnums.MetricAccuracy accuracy) {
        return switch (accuracy) {
            case days -> (BaseMapper<T>) metricDayMapper;
            case hours -> (BaseMapper<T>) metricHourMapper;
            default -> (BaseMapper<T>) metricMinuteMapper;
        };
    }

    public void eachDao(Consumer<BaseMapper<Metric>> consumer) {
        consumer.accept(chooseDao(MetricEnums.MetricAccuracy.days));
        consumer.accept(chooseDao(MetricEnums.MetricAccuracy.hours));
        consumer.accept(chooseDao(MetricEnums.MetricAccuracy.minutes));
    }
}
