package konnekt.prelude

@Target(AnnotationTarget.CLASS)
annotation class client

@Target(AnnotationTarget.FUNCTION)
annotation class HTTP

@Target(AnnotationTarget.FUNCTION)
annotation class GET(val value: String)

@Target(AnnotationTarget.FUNCTION)
annotation class DELETE(val value: String)

@Target(AnnotationTarget.FUNCTION)
annotation class HEAD

@Target(AnnotationTarget.FUNCTION)
annotation class PATCH

@Target(AnnotationTarget.FUNCTION)
annotation class POST(val value: String)

@Target(AnnotationTarget.FUNCTION)
annotation class PUT

@Target(AnnotationTarget.FUNCTION)
annotation class OPTIONS

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Path(val value: String, val encoded: Boolean = false)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Query(val value: String, val encoded: Boolean = false)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Body

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class QueryMap

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Field(val key: String)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class FieldMap

@Target(AnnotationTarget.FUNCTION)
annotation class Header

@Target(AnnotationTarget.FUNCTION)
annotation class HeaderMap

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Part

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class PartMap

//
annotation class Tag

annotation class Url

@Target(AnnotationTarget.FUNCTION)
annotation class Headers(vararg val values: String)

@Target(AnnotationTarget.FUNCTION)
annotation class Multipart

@Target(AnnotationTarget.FUNCTION)
annotation class FormUrlEncoded

