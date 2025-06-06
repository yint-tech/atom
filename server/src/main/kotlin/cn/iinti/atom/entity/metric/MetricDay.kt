package cn.iinti.atom.entity.metric

import com.baomidou.mybatisplus.annotation.TableName
import io.swagger.v3.oas.annotations.media.Schema
import lombok.Data
import lombok.EqualsAndHashCode
import lombok.experimental.Accessors

@Schema(name = "Metric对象", description = "监控指标")
@TableName("metric_day")
@Accessors(chain = true)
@Data
@EqualsAndHashCode(callSuper = false)
class MetricDay : Metric() {
}