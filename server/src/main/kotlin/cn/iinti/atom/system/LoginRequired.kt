package cn.iinti.atom.system

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.ANNOTATION_CLASS
import kotlin.annotation.AnnotationTarget.CONSTRUCTOR
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER
import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@Target(CLASS, FUNCTION, ANNOTATION_CLASS, CONSTRUCTOR, PROPERTY_GETTER, PROPERTY_SETTER)
annotation class LoginRequired(
    val forAdmin: Boolean = false,
    val apiToken: Boolean = false,
    val skipLogRecord: Boolean = false,
    val alert: Boolean = false,
    val value: String = ""
)