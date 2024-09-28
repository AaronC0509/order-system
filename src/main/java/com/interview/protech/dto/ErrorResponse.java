package com.interview.protech.dto;

import lombok.Data;

@Data
public class ErrorResponse {
    private final String errorCode;
    private final String message;

    public ErrorResponse(String errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
    }
}
