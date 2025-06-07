package cn.iinti.atom.service.base.perm

import cn.iinti.atom.entity.CommonRes
import cn.iinti.atom.service.base.dbcache.DbCacheManager
import cn.iinti.atom.system.AppContext
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.google.common.base.Function
import com.google.common.base.Splitter
import com.google.common.collect.HashMultimap
import com.google.common.collect.Maps
import com.google.common.collect.Sets
import com.google.common.io.LineReader
import jakarta.annotation.Resource
import org.slf4j.LoggerFactory
import org.springframework.boot.web.context.WebServerInitializedEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service
import java.io.StringReader
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@Service
class PermsService : ApplicationListener<WebServerInitializedEvent> {
    @Resource
    private lateinit var dbCacheManager: DbCacheManager

    @Suppress("rawtypes")
    private val allPermissionsWithScope = Maps.newHashMap<String, Permission<*>>()

    @Suppress("rawtypes")
    private val allPermissionWithType = Maps.newHashMap<Class<*>, Permission<*>>()

    companion object {
        private val log = LoggerFactory.getLogger(PermsService::class.java)
        private val splitter = Splitter.on(':').omitEmptyStrings().trimResults()

        
        fun rebuildExp(config: Map<String, Collection<String>>): String {
            val sb = StringBuilder()

            val sorted = TreeMap(config)

            sorted.forEach { (scope, permItems) ->
                var sbOfScope = StringBuilder().append(scope)
                for (item in permItems.sorted()) {
                    if (sbOfScope.length > 256) {
                        sb.append(sbOfScope).append("\n")
                        // 新建一个新的StringBuilder来避免重复引用同一个对象
                        val newSbOfScope = StringBuilder().append(scope)
                        newSbOfScope.append(":").append(item)
                        sbOfScope = newSbOfScope
                    } else {
                        sbOfScope.append(":").append(item)
                    }
                }
                sb.append(sbOfScope).append("\n")
            }
            return sb.toString()
        }

        @Suppress("UnstableApiUsage")
        
        fun parseExp(config: String, safe: Boolean): Map<String, Collection<String>> {
            val ret = HashMultimap.create<String, String>()
            val lineReader = LineReader(StringReader(config))
            var line: String?

            while (lineReader.readLine().also { line = it } != null) {
                if (line!!.isEmpty()) {
                    continue
                }
                val ref = AtomicReference<String?>(null)
                val count = AtomicInteger(0)
                splitter.splitToStream(line)
                    .forEach { s: String ->
                        if (ref.get() == null) {
                            ref.set(s)
                            return@forEach
                        }
                        ret.put(ref.get(), s)
                        count.incrementAndGet()
                    }

                if (count.get() == 0) {
                    if (safe) {
                        log.warn("error perms exp: {}", line)
                    } else {
                        throw IllegalArgumentException("error perms exp: $line")
                    }
                }
            }
            return ret.asMap()
        }
    }

    fun permissionScopes(): List<String> {
        return ArrayList(Sets.newTreeSet(allPermissionsWithScope.keys))
    }

    @Suppress("rawtypes", "unchecked")
    fun perms(scope: String): CommonRes<List<String>> {
        return CommonRes.ofPresent(allPermissionsWithScope[scope])
            .transform(Function { input ->
                val permission = input as? Permission<*>?
                val perms: Collection<String> = permission?.perms() ?: Collections.emptyList()
                ArrayList<String>(perms)
            })
    }

    fun <T> filter(type: Class<T>, sql: QueryWrapper<T>): QueryWrapper<T> {
        doAction(type, object : AuthAction<Void?, T> {
            override fun applyAuth(permission: Permission<T>, perms: Collection<String>): Void? {
                permission.filter(perms, sql)
                return null
            }

            override fun pass(): Void? {
                return null
            }
        })
        return sql
    }

    fun <T> filter(type: Class<T>, input: List<T>): List<T> {
        return doAction(type, object : AuthAction<List<T>, T> {
            override fun applyAuth(permission: Permission<T>, perms: Collection<String>): List<T> {
                return permission.filter(perms, input)
            }

            override fun pass(): List<T> {
                return input
            }
        })
    }

    fun <T> hasPermission(type: Class<T>, t: T): Boolean {
        return doAction(type, object : AuthAction<Boolean?, T> {
            override fun applyAuth(permission: Permission<T>, perms: Collection<String>): Boolean? {
                return permission.hasPermission(perms, t)
            }

            override fun pass(): Boolean? {
                return true
            }
        })!!
    }

    private fun <T1, T2> doAction(type: Class<T2>, function: AuthAction<T1, T2>): T1 {
        val user = AppContext.getUser()
        if (user == null || user.isAdmin!!) {
            return function.pass()!!
        }
        @Suppress("UNCHECKED_CAST")
        val permission = allPermissionWithType[type] as Permission<T2>?
        val userEx = dbCacheManager.userCacheWithName!!.getExtension(user.userName)
        if (permission != null) {
            val perms = userEx!!.perms.getOrDefault(permission.scope(), Collections.emptyList())
            return function.applyAuth(permission, perms)!!
        }
        throw IllegalStateException("no permission handler declared for type: $type")
    }

    private interface AuthAction<T1, T2> {
        fun applyAuth(permission: Permission<T2>, perms: Collection<String>): T1

        fun pass(): T1
    }


    override fun onApplicationEvent(event: WebServerInitializedEvent) {
        event.applicationContext.getBeansOfType(Permission::class.java)
            .values.forEach { permission ->
                allPermissionsWithScope[permission.scope()] = permission
                allPermissionWithType[permission.clazz] = permission
            }
    }
}