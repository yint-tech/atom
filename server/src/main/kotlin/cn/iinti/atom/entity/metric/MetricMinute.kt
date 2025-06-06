package cn.iinti.atom.entity.metric

import com.baomidou.mybatisplus.annotation.TableName
import io.swagger.v3.oas.annotations.media.Schema
import lombok.Data
import lombok.EqualsAndHashCode
import lombok.experimental.Accessors

@Schema(name = "Metric对象", description = "监控指标,分钟级")
@TableName("metric_minute")
@Accessors(chain = true)
@Data
@EqualsAndHashCode(callSuper = false)
class MetricMinute : Metric() {

}