package com.back.pinco.global.exception;

import com.back.pinco.global.rsData.RsData;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<RsData<Void>> handleServiceException(ServiceException e) {
        var errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(new RsData<>(
                        String.valueOf(errorCode.getCode()),
                        errorCode.getMessage()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RsData<Void>> handlePinValidationException(MethodArgumentNotValidException e) {
        FieldError firstError = e.getBindingResult().getFieldError();
        if (firstError == null) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new RsData<>("400", "잘못된 요청입니다."));
        }

        ValidationField field = ValidationField.from(firstError.getField());

        ErrorCode errorCode = switch (field) {
            case LatitudeField ignored -> ErrorCode.INVALID_PIN_LATITUDE;
            case LongitudeField ignored -> ErrorCode.INVALID_PIN_LONGITUDE;
            case ContentField ignored -> ErrorCode.INVALID_PIN_CONTENT;
            default -> ErrorCode.INVALID_VALUE;
        };

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(new RsData<>(
                        String.valueOf(errorCode.getCode()),
                        errorCode.getMessage()
                ));
    }

    //TODO : 다른 검증 값 오류도 정의하면 좋을듯

}