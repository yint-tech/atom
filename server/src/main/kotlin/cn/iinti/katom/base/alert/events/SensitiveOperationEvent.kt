package cn.iinti.katom.base.alert.events

data class SensitiveOperationEvent(
    val user: String,
    val api: String,
    val params: String
) {
    fun getMessage(): String {
        return """
                user: $user
                api: $api
                params: $params
                """.trimIndent()
    }
}