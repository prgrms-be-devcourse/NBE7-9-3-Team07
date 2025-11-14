package com.back.pinco.global.exception

sealed interface ValidationField {

    companion object {
        fun from(fieldName: String): ValidationField = when (fieldName) {
                "latitude" -> LatitudeField
                "longitude" -> LongitudeField
                "content" -> ContentField
                else -> UnknownField(fieldName)
        }
    }

    fun name(): String
}

data object LatitudeField : ValidationField {
    override fun name(): String = "latitude"
}

data object LongitudeField : ValidationField {
    override fun name(): String = "longitude"
}

data object ContentField : ValidationField {
    override fun name(): String = "content"
}

data class UnknownField(private val name: String) : ValidationField {
    override fun name() : String = name
}
