package com.back.pinco.global.exception

import lombok.Getter

@Getter
data class ServiceException(
    val errorCode: ErrorCode
) : RuntimeException("${errorCode.code}: ${errorCode.message}") {
}