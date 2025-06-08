package cn.iinti.katom.controller

import cn.iinti.katom.AtomMain
import cn.iinti.atom.BuildInfo
import cn.iinti.katom.entity.CommonRes
import cn.iinti.katom.entity.metric.Metric
import cn.iinti.katom.entity.metric.MetricTag
import cn.iinti.katom.mapper.metric.MetricTagMapper
import cn.iinti.katom.base.BroadcastService
import cn.iinti.katom.base.metric.mql.MQL
import cn.iinti.katom.system.LoginRequired
import cn.iinti.katom.base.metric.*
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.annotation.Resource
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@Tag(name = "监控指标相关")
@RequestMapping(BuildInfo.restfulApiPrefix + "/metric")
@Validated
class MetricController {

    @Resource
    lateinit var metricService: MetricService

    @Resource
    lateinit var metricTagService: MetricTagService

    @Resource
    lateinit var metricTagMapper: MetricTagMapper

    data class MetricQueryRequest(
        @field:NotBlank
        val name: String,
        val query: Map<String, String>? = null,
        @field:NotNull
        val accuracy: MetricEnums.MetricAccuracy
    )

    @Operation(summary = "查询指标,可以指定tag")
    @PostMapping("/queryMetric")
    @LoginRequired
    fun queryMetric(@RequestBody @Validated request: MetricQueryRequest): CommonRes<List<MetricVo>> {
        return CommonRes.success(metricService.queryMetric(request.name, request.query ?: emptyMap(), request.accuracy))
    }

    @Operation(summary = "查询指标,使用mql语言查询")
    @RequestMapping(value = ["/mqlQuery"], method = [RequestMethod.GET, RequestMethod.POST])
    @LoginRequired
    fun mqlQuery(@NotBlank @RequestParam("mqlScript") script: String, @NotNull @RequestParam("accuracy") accuracy: MetricEnums.MetricAccuracy): CommonRes<EChart4MQL> {
        return CommonRes.call { EChart4MQL.fromMQLResult(MQL.compile(script).run(accuracy, metricService)) }
    }

    @Operation(summary = "指标列表")
    @GetMapping("/metricNames")
    @LoginRequired
    fun metricNames(): CommonRes<List<String>> {
        return CommonRes.success(metricTagService.metricNames())
    }

    @Operation(summary = "指标Tag详情")
    @GetMapping("/metricTag")
    @LoginRequired
    fun queryMetricTag(@NotBlank @RequestParam("metricName") metricName: String): CommonRes<MetricTag> {
        return CommonRes.ofPresent(metricTagService.fromKey(metricName))
    }

    @Operation(summary = "所有的指标详情")
    @GetMapping("/allMetricConfig")
    @LoginRequired
    fun allMetricConfig(): CommonRes<List<MetricTag>> {
        return CommonRes.success(metricTagService.tagList())
    }

    @Operation(summary = "删除一个指标")
    @GetMapping("/deleteMetric")
    @LoginRequired
    fun deleteMetric(@NotBlank @RequestParam("metricName") metricName: String): CommonRes<String> {
        AtomMain.shardThread.post {
            metricService.eachDao { mapper ->
                mapper.delete(QueryWrapper<Metric>().eq(Metric.NAME, metricName))
            }
            metricTagMapper.delete(QueryWrapper<MetricTag>().eq(MetricTag.NAME, metricName))
            BroadcastService.triggerEvent(BroadcastService.Topic.METRIC_TAG)
            try {
                Thread.sleep(2000)
            } catch (e: InterruptedException) {
                return@post
            }
            metricService.eachDao { mapper ->
                mapper.delete(QueryWrapper<Metric>().eq(Metric.NAME, metricName))
            }
        }
        return CommonRes.success("ok")
    }
}