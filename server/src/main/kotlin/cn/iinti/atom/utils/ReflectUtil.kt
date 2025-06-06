package cn.iinti.atom.utils

import org.apache.commons.lang3.ClassUtils
import org.apache.commons.lang3.StringUtils
import java.lang.reflect.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class ReflectUtil {
    companion object {
        private val fieldCache = ConcurrentHashMap<ClassLoader, HashMap<String, Field?>>()

        /**
         * 每当有executor被加载的时候，就需要重置一下缓存，因为classloader需要被gc
         */
        fun clearCache() {
            fieldCache.clear()
        }

        fun getOrCreateFieldCache(classLoader: ClassLoader): HashMap<String, Field?> {
            return getOrCreateCache(classLoader, fieldCache)
        }


        fun <T> getOrCreateCache(
            classLoader: ClassLoader,
            cache: Map<ClassLoader, HashMap<String, T?>>
        ): HashMap<String, T?> {
            var loader = classLoader
            if (loader == null) {
                //jdk的class，对应classloader可能为null
                loader = ReflectUtil::class.java.classLoader
            }
            var ret = (cache as ConcurrentHashMap)[loader]
            if (ret != null) {
                return ret
            }
            synchronized(ReflectUtil::class.java) {
                ret = cache[loader]
                if (ret != null) {
                    return ret as HashMap<String, T?>
                }
                ret = HashMap<String, T?>()
                cache[loader] = ret!!
                return ret as HashMap<String, T?>
            }
        }

        private fun makeAccessible(field: Field) {
            if (!Modifier.isPublic(field.modifiers)) {
                field.isAccessible = true
            }
        }

        private fun getDeclaredField(`object`: Any, filedName: String): Field {
            return findField(`object`.javaClass, filedName)
        }

        fun setFieldValue(`object`: Any, fieldName: String, value: Any?) {
            try {
                findField(`object`.javaClass, fieldName)[`object`] = value
            } catch (e: IllegalAccessException) {
                throw IllegalStateException(e)
            }
        }

        @Suppress("UNCHECKED_CAST")
        fun <T> getFieldValue(`object`: Any, fieldName: String): T {
            try {
                return findField(`object`.javaClass, fieldName)[`object`] as T
            } catch (e: IllegalAccessException) {
                throw IllegalStateException(e)
            }
        }


        fun findClass(className: String, classLoader: ClassLoader?): Class<*> {
            val loader = classLoader ?: ReflectUtil::class.java.classLoader
            return try {
                ClassUtils.getClass(loader, className, false)
            } catch (e: ClassNotFoundException) {
                throw IllegalStateException(e)
            }
        }

        /**
         * Look up and return a class if it exists.
         * Like [findClass], but doesn't throw an exception if the class doesn't exist.
         *
         * @param className   The class name.
         * @param classLoader The class loader, or null for the boot class loader.
         * @return A reference to the class, or null if it doesn't exist.
         */
        fun findClassIfExists(className: String, classLoader: ClassLoader): Class<*>? {
            return try {
                findClass(className, classLoader)
            } catch (e: IllegalStateException) {
                null
            }
        }

        /**
         * Look up a field in a class and set it to accessible.
         *
         * @param clazz     The class which either declares or inherits the field.
         * @param fieldName The field name.
         * @return A reference to the field.
         * @throws NoSuchFieldError In case the field was not found.
         */
        fun findField(clazz: Class<*>, fieldName: String): Field {
            val fullFieldName = clazz.name + '#' + fieldName

            val classLoaderFieldCache = getOrCreateFieldCache(clazz.classLoader)


            if (classLoaderFieldCache.containsKey(fullFieldName)) {
                val field = classLoaderFieldCache[fullFieldName]
                if (field == null)
                    throw NoSuchFieldError(fullFieldName)
                return field
            }

            return try {
                val field = findFieldRecursiveImpl(clazz, fieldName)
                field.isAccessible = true
                classLoaderFieldCache[fullFieldName] = field
                field
            } catch (e: NoSuchFieldException) {
                classLoaderFieldCache[fullFieldName] = null
                throw NoSuchFieldError(fullFieldName)
            }
        }

        /**
         * Look up and return a field if it exists.
         * Like [findField], but doesn't throw an exception if the field doesn't exist.
         *
         * @param clazz     The class which either declares or inherits the field.
         * @param fieldName The field name.
         * @return A reference to the field, or null if it doesn't exist.
         */
        fun findFieldIfExists(clazz: Class<*>, fieldName: String): Field? {
            return try {
                findField(clazz, fieldName)
            } catch (e: NoSuchFieldError) {
                null
            }
        }

        private fun findFieldRecursiveImpl(clazz: Class<*>, fieldName: String): Field {
            return try {
                clazz.getDeclaredField(fieldName)
            } catch (e: NoSuchFieldException) {
                var currentClazz = clazz.superclass
                while (currentClazz != null && currentClazz != Object::class.java) {
                    try {
                        return currentClazz.getDeclaredField(fieldName)
                    } catch (ignored: NoSuchFieldException) {
                    }
                    currentClazz = currentClazz.superclass
                }
                throw e
            }
        }


        /**
         * Maps a primitive class name to its corresponding abbreviation used in array class names.
         */
        private val abbreviationMap = mapOf(
            "int" to "I",
            "boolean" to "Z",
            "float" to "F",
            "long" to "J",
            "short" to "S",
            "byte" to "B",
            "double" to "D",
            "char" to "C"
        )

        /**
         * Feed abbreviation maps
         */
        private val reverseAbbreviationMap = abbreviationMap.map { it.value to it.key }.toMap()

        /**
         * Converts a class name to a JLS style class name.
         * @param className the class name
         * @return the converted name
         */
        private fun toCanonicalName(className: String): String {
            var className = className
            className = deleteWhitespace(className) ?: throw IllegalArgumentException("className must not be null")
            if (className.endsWith("[]")) {
                val classNameBuffer = StringBuilder()
                while (className.endsWith("[]")) {
                    className = className.substring(0, className.length - 2)
                    classNameBuffer.append("[")
                }
                val abbreviation = abbreviationMap[className]
                if (abbreviation != null) {
                    classNameBuffer.append(abbreviation)
                } else {
                    classNameBuffer.append("L").append(className).append(";")
                }
                className = classNameBuffer.toString()
            }
            return className
        }

        /**
         * Deletes all whitespaces from a String as defined by [Character.isWhitespace].
         * @param str the String to delete whitespace from, may be null
         * @return the String without whitespaces, null if null String input
         */
        fun deleteWhitespace(str: String?): String? {
            if (StringUtils.isEmpty(str)) {
                return str
            }
            val sz = str!!.length
            val chs = CharArray(sz)
            var count = 0
            for (i in 0 until sz) {
                if (!Character.isWhitespace(str[i])) {
                    chs[count++] = str[i]
                }
            }
            if (count == sz) {
                return str
            }
            return String(chs, 0, count)
        }

        // Additional methods omitted for brevity
    }
}