package com.smarthelpdesk.apigateway.exception;

import org.apache.coyote.BadRequestException;

public class ValidationException extends BaseException {
    public ValidationException(String message){
        super(message);
    }
}
