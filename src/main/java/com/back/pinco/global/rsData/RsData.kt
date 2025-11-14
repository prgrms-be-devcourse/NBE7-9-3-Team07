package com.back.pinco.global.rsData;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class RsData<T> {

    private String errorCode;
    private String msg;
    private T data;

    public RsData(String errorCode, String msg) {
        this.errorCode = errorCode;
        this.msg = msg;
        this.data = null;
    }

    @JsonIgnore
    public int getStatusCode() {
        String statusCode = errorCode.split("-")[0];
        return Integer.parseInt(statusCode);
    }

}