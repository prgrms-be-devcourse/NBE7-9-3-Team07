package com.back.pinco.global.rsData

import com.fasterxml.jackson.annotation.JsonIgnore
import lombok.AllArgsConstructor
import lombok.Getter

data class RsData<T>(
    val errorCode: String,
    val msg: String,
    val data: T? = null
) {
    @get:JsonIgnore
    val statusCode: Int
        get() = errorCode.substringBefore("-").toInt();
}