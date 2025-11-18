package com.back.pinco.global.exception


data class ServiceException(
    val errorCode: ErrorCode
) : RuntimeException("${errorCode.code}: ${errorCode.message}") {
}