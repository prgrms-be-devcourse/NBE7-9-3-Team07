package com.back.pinco.global.exception;

import lombok.Getter;

@Getter
public class ServiceException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * @param errorCode ErrorCode.ENUM 값
     */
    public ServiceException(ErrorCode errorCode) {
        super("%d : %s".formatted(errorCode.getCode(), errorCode.getMessage()));
        this.errorCode = errorCode;
    }
}