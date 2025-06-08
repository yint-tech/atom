package cn.iinti.katom.base.config

import cn.iinti.katom.entity.CommonRes
import cn.iinti.katom.entity.SysConfig
import cn.iinti.katom.mapper.SysConfigMapper
import cn.iinti.katom.base.BroadcastService
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import jakarta.annotation.Resource
import org.springframework.stereotype.Service

@Service
class ConfigService {
    companion object {
        const val SYSTEM_SETTINGS = "__atom_system_setting"
    }

    @Resource
    private lateinit var sysConfigMapper: SysConfigMapper

    fun allConfig(): CommonRes<List<SysConfig>> {
        return CommonRes.success(
            sysConfigMapper.selectList(
                QueryWrapper<SysConfig>()
                    .eq(SysConfig.CONFIG_COMMENT, SYSTEM_SETTINGS)
            )
        )
    }

    fun setConfig(key: String, value: String): CommonRes<SysConfig> {
        if (key.startsWith("__")) {
            return CommonRes.failed("can not setup system internal properties :$key")
        }
        val sysConfig = sysConfigMapper.selectOne(
            QueryWrapper<SysConfig>()
                .eq(SysConfig.CONFIG_KEY, key)
        ) ?: SysConfig()

        sysConfig.configKey = key
        sysConfig.configValue = value
        sysConfig.configComment = SYSTEM_SETTINGS
        if (sysConfig.id == null) {
            sysConfigMapper.insert(sysConfig)
        } else {
            sysConfigMapper.updateById(sysConfig)
        }
        BroadcastService.triggerEvent(BroadcastService.Topic.CONFIG)
        return CommonRes.success(sysConfig)
    }

    fun reloadConfig() {
        Configs.refreshConfig(
            sysConfigMapper.selectList(
                QueryWrapper<SysConfig>()
                    .eq(SysConfig.CONFIG_COMMENT, SYSTEM_SETTINGS)
            )
        )
    }
}