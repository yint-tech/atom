package cn.iinti.atom.service.base.config

import com.google.common.collect.Sets
import org.apache.commons.lang3.StringUtils
import java.net.MalformedURLException
import java.net.URL
import java.util.*

/**
 * 校验设置配置中前端传递过来的参数
 */
object SettingsValidate {
    fun doValidate(key: String?, value: String?): String? {
        if (StringUtils.isBlank(key)) {
            return "key is blank"
        }
        val validator = validatorMap[key]
        if (validator != null) {
            val error = validator.doValidate(value)
            if (error != null) {
                return error
            }
        }
        return Configs.validateConfig(key!!, value)
    }

    private val validatorMap = HashMap<String, Validator>().apply {
        put(Settings.outIpTestUrl.key, URLValidator())
    }

    fun registerValidator(key: String, validator: Validator) {
        validatorMap[key] = validator
    }

    interface Validator {
        fun doValidate(value: String?): String?
    }

    private class EnumValidator(enums: Collection<String>) : Validator {
        private val enums = Sets.newHashSet(enums)

        override fun doValidate(value: String?): String? {
            if (enums.contains(value)) {
                return null
            }
            return "not in enum list: $value"
        }
    }

    private class URLValidator : Validator {

        override fun doValidate(value: String?): String? {
            if (StringUtils.isBlank(value)) {
                return null
            }
            try {
                URL(value)
                return null
            } catch (e: MalformedURLException) {
                return e.message
            }
        }
    }

    private class IntegerRange(private val min: Int, private val max: Int) : Validator {

        override fun doValidate(value: String?): String? {
            if (value == null) {
                return null
            }
            try {
                val i = value.toInt()
                if (i < min) {
                    return "value: $value must grater :$min"
                }
                if (i > max) {
                    return "value: $value must less :$max"
                }
                return null
            } catch (e: NumberFormatException) {
                return e.message
            }
        }
    }
}