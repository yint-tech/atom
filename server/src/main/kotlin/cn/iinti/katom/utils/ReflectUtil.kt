package cn.iinti.katom.utils

import java.lang.reflect.Field
import java.util.concurrent.ConcurrentHashMap

class ReflectUtil {
    companion object {
        private val fieldCache = ConcurrentHashMap<ClassLoader, HashMap<String, Field?>>()

        fun getOrCreateFieldCache(classLoader: ClassLoader?): HashMap<String, Field?> {
            return getOrCreateCache(classLoader, fieldCache)
        }


        fun <T> getOrCreateCache(
            classLoader: ClassLoader?,
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
                ret = HashMap()
                cache[loader] = ret!!
                return ret as HashMap<String, T?>
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
    }
}