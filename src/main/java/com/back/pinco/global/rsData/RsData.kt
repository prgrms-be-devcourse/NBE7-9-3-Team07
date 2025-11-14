package com.back.pinco.global.rsData

import com.fasterxml.jackson.annotation.JsonIgnore
import lombok.AllArgsConstructor
import lombok.Getter

data class RsData<T>(
    private val errorCode: String,
    private val msg: String,
    private val data: T? = null
) {
    @get:JsonIgnore
    val statusCode: Int
        get() = errorCode.substringBefore("-").toInt();
}