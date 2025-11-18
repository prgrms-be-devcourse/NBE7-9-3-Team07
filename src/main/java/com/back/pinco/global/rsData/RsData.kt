package com.back.pinco.global.rsData

import com.fasterxml.jackson.annotation.JsonIgnore

data class RsData<T>(
    val errorCode: String,
    val msg: String,
    val data: T? = null
) {
    @get:JsonIgnore
    val statusCode: Int
        get() = errorCode.substringBefore("-").toInt();
}