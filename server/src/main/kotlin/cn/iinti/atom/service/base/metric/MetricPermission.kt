package cn.iinti.atom.service.base.metric

import cn.iinti.atom.entity.metric.Metric
import cn.iinti.atom.service.base.perm.Permission
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import org.springframework.stereotype.Component


/**
 * 控制用户查看指标的权限,
 * 只是一个demo，实际上不应该让普通用户有查看指标的权限
 */
@Component
class MetricPermission : Permission<Metric>(Metric::class.java) {
    private val categoryRegistry: MutableSet<String> = HashSet()
    private val metricMapRegistry: Multimap<String, String?> = HashMultimap.create()

    init {
        addDefault("订单模块", "atom.order.create")
        addDefault("订单模块", "atom.order.cancel")

        addDefault("用户模块", "atom.user.login")
        addDefault("用户模块", "atom.user.register")
    }

    private fun addDefault(category: String, metricName: String) {
        categoryRegistry.add(category)
        metricMapRegistry.put(category, metricName)
    }

    override fun scope(): String {
        return "metric"
    }

    override fun perms(): Collection<String> {
        return categoryRegistry
    }

    override fun filter(perms: Collection<String>, sql: QueryWrapper<Metric>) {
        if (perms.isEmpty()) {
            sql.eq(Metric.ID, -1)
            return
        }
        val hasPermsMetrics: MutableSet<String?> = HashSet()
        for (perm in perms) {
            val metricNames = metricMapRegistry[perm]
            hasPermsMetrics.addAll(metricNames)
        }
        if (hasPermsMetrics.isEmpty()) {
            sql.eq(Metric.ID, -1)
            return
        }

        sql.and { metricQueryWrapper: QueryWrapper<Metric?> ->
            metricQueryWrapper.`in`(
                Metric.NAME,
                hasPermsMetrics
            )
        }
    }


    override fun hasPermission(perms: Collection<String>, t: Metric): Boolean {
        return perms.any { t.name!!.startsWith(it) }
    }
}
