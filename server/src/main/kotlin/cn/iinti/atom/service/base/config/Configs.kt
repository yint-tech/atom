package cn.iinti.atom.service.base.config

import cn.iinti.atom.entity.SysConfig
import cn.iinti.atom.service.base.env.Environment
import cn.iinti.atom.utils.CommonUtils
import com.alibaba.fastjson.parser.ParserConfig
import com.alibaba.fastjson.util.TypeUtils
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.google.common.collect.Sets
import org.apache.commons.lang3.ObjectUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.math.NumberUtils
import java.io.IOException
import java.lang.reflect.ParameterizedType
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author
 * @create
 */
private val log = org.slf4j.LoggerFactory.getLogger(Configs::class.java)

object Configs {
    private val applicationProperties = Properties().apply {
        try {
            val stream = Configs::class.java.classLoader.getResourceAsStream(Environment.APPLICATION_PROPERTIES)
            if (stream != null) {
                load(stream)
            }
        } catch (e: IOException) {
            throw IllegalStateException(e)
        }
    }

    private var dbConfigs = Maps.newHashMap<String, String>()

    
    fun getConfig(key: String, defaultValue: String): String? {
        val config = getConfig(key)
        return if (StringUtils.isBlank(config)) {
            defaultValue
        } else {
            config
        }
    }

    fun getConfig(key: String): String? {
        if (dbConfigs.containsKey(key)) {
            // 无论如何，只要数据库存在此项配置，那么一定使用数据库
            // 实现一个完善的 overlay 配置中心是非常困难的
            return dbConfigs[key]
        }
        return applicationProperties.getProperty(key)
    }

    fun refreshConfig(configList: List<SysConfig>) {
        val copyOnWriteConfigs = HashMap<String, String>()
        for (sysConfig in configList) {
            copyOnWriteConfigs[sysConfig.configKey!!] = sysConfig.configValue!!
        }
        // diff and notify config change listener
        var hasChange = false
        if (dbConfigs.size != copyOnWriteConfigs.size) {
            hasChange = true
        } else {
            for (key in dbConfigs.keys) {
                if (!copyOnWriteConfigs.containsKey(key)) {
                    hasChange = true
                    break
                }
                if (!Objects.equals(dbConfigs[key], copyOnWriteConfigs[key])) {
                    hasChange = true
                    break
                }
            }
        }
        dbConfigs = copyOnWriteConfigs
        if (hasChange) {
            notifyConfigChange()
        }
    }

    fun getInt(key: String, defaultValue: Int): Int {
        val config = getConfig(key)
        return if (StringUtils.isBlank(config)) {
            defaultValue
        } else {
            NumberUtils.toInt(config, defaultValue)
        }
    }

    fun interface ConfigChangeListener {
        fun onConfigChange()
    }

    private val sListenerCount = AtomicInteger()
    private val configChangeListeners = Sets.newConcurrentHashSet<ConfigChangeListener>()
    private const val MAX_LISTENER_COUNT = 256

    fun addConfigChangeListener(listener: ConfigChangeListener) {
        if (sListenerCount.incrementAndGet() > MAX_LISTENER_COUNT) {
            // 高并发情况下，监听器理论上都应该是静态的
            // 否则容易让那个这个对象被撑爆
            throw IllegalStateException("to many config change listener register")
        }
        if (sListenerCount.get() > MAX_LISTENER_COUNT / 2) {
            log.warn("to many config change listener register")
        }
        configChangeListeners.add(listener)
    }

    private fun notifyConfigChange() {
        val pending = Lists.newLinkedList<ConfigChangeListener>()
        for (listener in configChangeListeners) {
            if (listener is MonitorConfigChangeListener<*>) {
                // monitor需要先执行
                listener.onConfigChange()
            } else {
                pending.add(listener)
            }
        }
        for (listener in pending) {
            listener.onConfigChange()
        }
    }

    interface ConfigFetcher<T> {
        fun fetch(value: T?)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> getSuperClassGenericType(clazz: Class<*>): Class<T> {
        val genType = clazz.genericSuperclass
        if (genType !is ParameterizedType) {
            return Object::class.java as Class<T>
        }
        val params = genType.actualTypeArguments
        return if (params.isEmpty()) {
            Object::class.java as Class<T>
        } else if (params[0] !is Class<*>) {
            Object::class.java as Class<T>
        } else {
            params[0] as Class<T>
        }
    }

    
    @Suppress("UNCHECKED_CAST")
    fun <T> addConfigFetcher(
        configFetcher: ConfigFetcher<T?>,
        configKey: String,
        defaultValue: T?,
        transformer: TransformFunc<T?>?,
        valueType: Class<T>?
    ) {
        var resolveValueType: Class<T> = valueType ?: getSuperClassGenericType(configFetcher.javaClass)

        if (resolveValueType == Object::class.java && defaultValue != null) {
            resolveValueType = defaultValue.javaClass as Class<T>
        }
        if (resolveValueType == Object::class.java && transformer != null) {
            resolveValueType = transformer.javaClass as Class<T>
        }


        val configChangeListener = MonitorConfigChangeListener(
            configKey, defaultValue, configFetcher, transformer, resolveValueType as Class<T?>
        )
        configChangeListener.onConfigChange()
        addConfigChangeListener(configChangeListener)
    }

    private val autoTransformerValidators = HashMap<String, MonitorConfigChangeListener<*>>()

    fun validateConfig(key: String, value: String?): String? {
        val monitorConfigChangeListener = autoTransformerValidators[key] ?: return null
        return try {
            monitorConfigChangeListener.transform(value)
            null
        } catch (throwable: Throwable) {
            log.warn("config validation error", throwable)
            CommonUtils.throwableToString(throwable)
        }
    }

    class MonitorConfigChangeListener<T>(
        private val configKey: String,
        private val defaultValue: T?,
        private val configFetcher: ConfigFetcher<T>,
        private val transformer: TransformFunc<T>?,
        private val finalValueType: Class<T>
    ) : ConfigChangeListener {
        private var originValue: String? = null
        private var value: T? = null

        init {
            autoTransformerValidators[configKey] = this as MonitorConfigChangeListener<*>
        }

        @Suppress("UNCHECKED_CAST")
        fun transform(config: String?): T {
            return transformer?.apply(config, finalValueType)
                ?: (defaultTransformFuncInstance.apply(
                    config,
                    finalValueType as Class<Any>
                ) as T)
        }

        override fun onConfigChange() {
            val config = getConfig(configKey)
            if (value != null && Objects.equals(originValue, config)) {
                return
            }
            originValue = config
            if (config == null && defaultValue != null) {
                value = defaultValue
                configFetcher.fetch(defaultValue)
                return
            }
            val t = transform(config)
            value = t
            configFetcher.fetch(t)
        }
    }

    fun interface TransformFunc<T> {
        fun apply(value: String?, type: Class<T>): T
    }


    val defaultTransformFuncInstance =
        TransformFunc<Any> { value, type -> TypeUtils.cast(value, type, ParserConfig.getGlobalInstance()) }

    private val registerConfigValueRecord = Maps.newConcurrentMap<String, Any>()

    open class ConfigValue<V>(
        val key: String,
        defaultValue: V
    ) {
        var value: V? = defaultValue
            protected set
        var sValue: String? = null
            protected set

        open fun configType(): String {
            return (value as Any).javaClass.simpleName ?: ""
        }

        protected open fun transformer(): TransformFunc<V?>? {
            return null
        }

        init {
            if (registerConfigValueRecord.containsKey(key)) {
                val o = registerConfigValueRecord[key]
                if (!ObjectUtils.notEqual(o, defaultValue)) {
                    // 系统的自动包装监听器在使用的时候不能出现重复，
                    // 这里主要是默认值的设置可能存在歧义，我们认为配置中心在配置缺失状态下默认值策略应该是一致的
                    // 所以这里发现存在相同的配置key的时候，检查一下默认值，如果默认值不相同，那么认为这是错误的
                    throw IllegalStateException(
                        "duplicate config key monitor registered key:$key" +
                                "defaultValue1:$o" +
                                "defaultValue2:$defaultValue" +
                                "monitorClass:${this.javaClass}"
                    )
                }
            }
            registerConfigValueRecord[key] = defaultValue

            val superClassGenericType: Class<V> = getSuperClassGenericType(javaClass)
            val transformer = transformer()

            addConfigFetcher(
                object : ConfigFetcher<V?> {
                    override fun fetch(value: V?) {
                        this@ConfigValue.value = value!!
                        sValue = getConfig(key)
                        if (sValue == null && this@ConfigValue.value is String) {
                            sValue = value as String
                        }
                    }
                },
                key,
                defaultValue,
                transformer,
                superClassGenericType
            )
        }
    }

    class StringConfigValue(key: String, defaultValue: String) : ConfigValue<String>(key, defaultValue)

    class MultiLineStrConfigValue(key: String, defaultValue: String) : ConfigValue<String>(key, defaultValue) {
        override fun configType(): String {
            return "multiLine"
        }
    }

    class BooleanConfigValue(key: String, defaultValue: Boolean) : ConfigValue<Boolean>(key, defaultValue)

    class IntegerConfigValue(key: String, defaultValue: Int) : ConfigValue<Int>(key, defaultValue)

    /**
     * 可变的数字抽象，比IntegerConfigValue更好用，因为他是jdk标准抽象
     */
    class NumberIntegerConfigValue(key: String, defaultValue: Int) : Number() {
        private val configValue = IntegerConfigValue(key, defaultValue)


        override fun toByte(): Byte {
            return configValue.value!!.toByte()
        }

        override fun toDouble(): Double {
            return configValue.value!!.toDouble()
        }

        override fun toFloat(): Float {
            return configValue.value!!.toFloat()
        }

        override fun toInt(): Int {
            return configValue.value!!.toInt()
        }

        override fun toLong(): Long {
            return configValue.value!!.toLong()
        }

        override fun toShort(): Short {
            return configValue.value!!.toShort()
        }
    }

    fun noneBlank(collection: Iterable<StringConfigValue>): Boolean {
        for (value in collection) {
            if (StringUtils.isBlank(value.value)) {
                return false
            }
        }
        return true
    }

    
    fun addKeyMonitor(key: String, changeListener: ConfigChangeListener) {
        addKeyMonitor(listOf(key), changeListener)
    }

    /**
     * 监控一些配置的变更，只有当对应的key发生了变化的时候才触发监听函数
     *
     * @param keys           key列表
     * @param changeListener 事件监听
     */
    
    fun addKeyMonitor(keys: List<String>, changeListener: ConfigChangeListener) {
        val data = Maps.newHashMap<String, String>()
        for (key in keys) {
            data[key] = getConfig(key)!!
        }
        addConfigChangeListener {
            var hasChange = false
            for (key in keys) {
                val nowValue = getConfig(key)
                if (!Objects.equals(data[key], nowValue)) {
                    hasChange = true
                    data[key] = nowValue
                }
            }
            if (hasChange) {
                changeListener.onConfigChange()
            }
        }
    }
}