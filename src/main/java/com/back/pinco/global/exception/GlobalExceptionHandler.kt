package com.back.pinco.global.exception

import com.back.pinco.global.rsData.RsData
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException::class)
    fun handleServiceException(e: ServiceException): ResponseEntity<RsData<Unit>> {
        val errorCode = e.errorCode

        return ResponseEntity.status(errorCode.status).body(
                RsData(
                    errorCode.code.toString(), errorCode.message
                )
            )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handlePinValidationException(e: MethodArgumentNotValidException): ResponseEntity<RsData<Unit>> {
        val firstError = e.bindingResult.fieldError ?: return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(RsData("400", "잘못된 요청입니다."))

        val field = ValidationField.from(firstError.field)

        val errorCode = when (field) {
            is LatitudeField -> ErrorCode.INVALID_PIN_LATITUDE
            is LongitudeField -> ErrorCode.INVALID_PIN_LONGITUDE
            is ContentField -> ErrorCode.INVALID_PIN_CONTENT
            is UnknownField -> ErrorCode.INVALID_VALUE
        }

        return ResponseEntity
            .status(errorCode.status)
            .body(
                RsData(
                    errorCode.code.toString(),
                    errorCode.message
                )
            )
    } //TODO : 다른 검증 값 오류도 정의하면 좋을듯

    // HttpMessageNotReadableException 예외 처리 (요청 바디의 json 형식 오류)
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleJsonParseException(e: HttpMessageNotReadableException): ResponseEntity<RsData<Unit>> {

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(RsData("400", "잘못된 요청입니다."))
    }
}