package cn.iinti.atom.service.base.metric

import cn.iinti.atom.entity.metric.Metric
import cn.iinti.atom.entity.metric.MetricTag
import cn.iinti.atom.mapper.metric.MetricTagMapper
import cn.iinti.atom.service.base.BroadcastService
import cn.iinti.atom.service.base.BroadcastService.Companion.register
import cn.iinti.atom.service.base.metric.MetricEnums.TimeSubType
import cn.iinti.atom.utils.Md5Utils.md5Hex
import cn.iinti.atom.utils.ServerIdentifier.id
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.Tag
import jakarta.annotation.PostConstruct
import jakarta.annotation.Resource
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import java.util.*
import java.util.stream.Collectors


@Service
class MetricTagService {
    private val tagMap: MutableMap<String, MetricTag> = Maps.newConcurrentMap()

    @Resource
    private val metricTagMapper: MetricTagMapper? = null


    @PostConstruct
    fun loadAll() {
        metricTagMapper!!.selectList(QueryWrapper()).forEach { metricTag: MetricTag ->
            tagMap[metricTag.name!!] = metricTag
        }

        register(BroadcastService.Topic.METRIC_TAG) {
            tagMap.clear()
            metricTagMapper.selectList(QueryWrapper()).forEach { metricTag: MetricTag ->
                tagMap[metricTag.name!!] = metricTag
            }
        }
    }

    fun metricNames(): List<String> {
        return ArrayList(TreeSet(tagMap.keys))
    }

    fun tagList(): List<MetricTag> {
        val metricTags = ArrayList(tagMap.values)
        metricTags.sortBy { it.name }
        return metricTags
    }

    fun fromKey(metricName: String?): MetricTag? {
        if (tagMap.containsKey(metricName)) {
            return tagMap[metricName]
        }
        return metricTagMapper!!.selectOne(QueryWrapper<MetricTag>().eq(MetricTag.NAME, metricName))
    }

    fun fromMeter(meter: Meter, timerType: TimeSubType?): MetricTag {
        val meterId = meter.id
        val key = meterId.name
        if (tagMap.containsKey(key)) {
            return tagMap[key]!!
        }

        var metricTag = metricTagMapper!!.selectOne(QueryWrapper<MetricTag>().eq(MetricTag.NAME, key))
        if (metricTag == null) {
            metricTag = createTag(key, wrapTagList(meterId.tags, timerType))
        }
        tagMap[key] = metricTag
        return metricTag
    }

    private fun wrapTagList(list: List<Tag>, timerType: TimeSubType?): List<Tag> {
        val ret = Lists.newArrayListWithExpectedSize<Tag>(list.size + 1)
        ret.add(Tag.of(TAG_SERVER_ID, id()))
        if (timerType != null) {
            ret.add(Tag.of(TAG_TIMER_TYPE, timerType.metricKey))
        }
        ret.addAll(list)
        return ret
    }

    fun <T : Metric?> wrapQueryWithTags(
        queryWrapper: QueryWrapper<T>,
        tags: Map<String, String?>?,
        metricTag: MetricTag
    ): QueryWrapper<T> {
        if (tags == null || tags.isEmpty()) {
            return queryWrapper
        }
        val tag1Name = metricTag.tag1Name
        var s = tags[tag1Name]
        if (s != null) {
            queryWrapper.eq(Metric.TAG1, s)
        }

        val tag2Name = metricTag.tag2Name
        s = tags[tag2Name]
        if (s != null) {
            queryWrapper.eq(Metric.TAG2, s)
        }

        val tag3Name = metricTag.tag3Name
        s = tags[tag3Name]
        if (s != null) {
            queryWrapper.eq(Metric.TAG3, s)
        }

        val tag4Name = metricTag.tag4Name
        s = tags[tag4Name]
        if (s != null) {
            queryWrapper.eq(Metric.TAG4, s)
        }

        val tag5Name = metricTag.tag5Name
        s = tags[tag5Name]
        if (s != null) {
            queryWrapper.eq(Metric.TAG5, s)
        }
        return queryWrapper
    }

    fun setupTag(metricTag: MetricTag, meter: Meter, metric: Metric, timerType: TimeSubType?) {
        val uniformKey = StringBuilder(meter.id.name)

        wrapTagList(meter.id.tags, timerType)
            .stream()
            .sorted(Comparator.comparing<Tag, String> { obj: Tag -> obj.key })
            .forEach { tag: Tag ->
                val key = tag.key
                uniformKey.append(key).append(tag.value).append("&")
                if (key == metricTag.tag1Name) {
                    metric.tag1 = tag.value
                } else if (key == metricTag.tag2Name) {
                    metric.tag2 = tag.value
                } else if (key == metricTag.tag3Name) {
                    metric.tag3 = tag.value
                } else if (key == metricTag.tag4Name) {
                    metric.tag4 = tag.value
                } else if (key == metricTag.tag5Name) {
                    metric.tag5 = tag.value
                }
            }
        metric.tagsMd5 = md5Hex(uniformKey.toString())
    }

    private fun createTag(key: String, tags: List<Tag>): MetricTag {
        var tags = tags
        check(tags.size <= TAG_SLOT_COUNT) { "tags size must less than :" + TAG_SLOT_COUNT }
        val metricTag = MetricTag()
        metricTag.name = key
        tags = tags.stream().sorted(Comparator.comparing { obj: Tag -> obj.key }).collect(Collectors.toList())

        for (i in tags.indices) {
            val tagName = tags[i].key
            when (i) {
                0 -> metricTag.tag1Name = tagName
                1 -> metricTag.tag2Name = tagName
                2 -> metricTag.tag3Name = tagName
                3 -> metricTag.tag4Name = tagName
                4 -> metricTag.tag5Name = tagName
            }
        }
        try {
            metricTagMapper!!.insert(metricTag)
        } catch (ignore: DuplicateKeyException) {
        }
        return metricTagMapper!!.selectOne(QueryWrapper<MetricTag>().eq(MetricTag.NAME, key))
    }

    companion object {
        private const val TAG_SERVER_ID = "serverId"

        private const val TAG_TIMER_TYPE = TimeSubType.TIMER_TYPE

        private const val TAG_SLOT_COUNT = 5
    }
}
