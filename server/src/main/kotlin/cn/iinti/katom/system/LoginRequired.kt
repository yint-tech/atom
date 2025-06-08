package cn.iinti.katom.system

import kotlin.annotation.AnnotationTarget.*

@Retention(AnnotationRetention.RUNTIME)
@Target(CLASS, FUNCTION, ANNOTATION_CLASS, CONSTRUCTOR, PROPERTY_GETTER, PROPERTY_SETTER)
annotation class LoginRequired(
    val forAdmin: Boolean = false,
    val apiToken: Boolean = false,
    val skipLogRecord: Boolean = false,
    val alert: Boolean = false,
    val value: String = ""
)