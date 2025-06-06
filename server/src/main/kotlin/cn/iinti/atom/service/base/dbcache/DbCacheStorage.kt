package cn.iinti.atom.service.base.dbcache

import cn.iinti.atom.utils.ReflectUtil
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.google.common.collect.Maps
import java.util.*
import org.springframework.scheduling.annotation.Scheduled

class DbCacheStorage<M, E>(val keyField: String, val baseMapper: BaseMapper<M>, private val updateHandler: UpdateHandler<M, E>? = null) {
    private var cacheMap: MutableMap<String, DbWrapper<M, E>> = Maps.newConcurrentMap()
    private val keyFieldCamel: String = lineToCamel(keyField)

    fun getModeWithCache(key: String?): M? {
        if (key == null) {
            return null
        }
        val wrapper = cacheMap[key]
        return wrapper?.m
    }

    fun getMode(key: String?): M? {
        val wrapper = getWrapper(key)
        return wrapper?.m
    }

    fun getExtension(key: String?): E? {
        val wrapper = getWrapper(key)
        return wrapper?.e
    }

    fun getWrapper(key: String?): DbWrapper<M, E>? {
        if (key == null) {
            return null
        }
        var mDbWrapper = cacheMap[key]
        if (mDbWrapper != null) {
            return mDbWrapper
        }
        val m = baseMapper.selectOne(QueryWrapper<M>().eq(keyField, key))
        if (m != null) {
            mDbWrapper = DbWrapper(m)
            if (updateHandler != null) {
                mDbWrapper.e = updateHandler.doUpdate(m, null)
            }
            cacheMap[key] = mDbWrapper
        }
        return mDbWrapper
    }

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    fun updateAll() {
        val newCacheMap: MutableMap<String, DbWrapper<M, E>> = Maps.newConcurrentMap()
        baseMapper.selectList(QueryWrapper<M>()).forEach { m ->
            var keyObj: Any?
            try {
                keyObj = ReflectUtil.getFieldValue(m!!, keyFieldCamel)
            } catch (error: NoSuchFieldError) {
                keyObj = ReflectUtil.getFieldValue(m!!, keyField)
            }
            if (keyObj == null) {
                return@forEach
            }
            val key = keyObj.toString()

            if (key.isBlank()) {
                return@forEach
            }
            var wrapper = cacheMap[key]
            if (wrapper != null) {
                if (updateHandler != null) {
                    wrapper.e = updateHandler.doUpdate(m as M, wrapper.e)
                }
                // 将val改为var并添加显式类型转换
                var tempM: M = m as M
                wrapper.m = tempM
            } else {
                wrapper = DbWrapper(m as M)
                if (updateHandler != null) {
                    wrapper.e = updateHandler.doUpdate(m as M, null)
                }
            }
            newCacheMap[key] = wrapper
        }
        cacheMap = newCacheMap
    }

    fun extensions(): Collection<E> {
        return cacheMap.values.map { it.e }.filterNotNull()
    }

    fun models(): List<M> {
        return cacheMap.values.map { it.m }.filterNotNull()
    }

    interface UpdateHandler<M, E> {
        fun doUpdate(m: M, e: E?): E
    }

    companion object {
        @JvmStatic
        fun lineToCamel(str: String): String {
            val sb = StringBuilder()
            var preUnderLine = false
            for (ch in str.toCharArray()) {
                if (ch == '_') {
                    preUnderLine = true
                    continue
                }
                sb.append(if (preUnderLine) Character.toUpperCase(ch) else ch)
                preUnderLine = false
            }
            return sb.toString()
        }
    }
}