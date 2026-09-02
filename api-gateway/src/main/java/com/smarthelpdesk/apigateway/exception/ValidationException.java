package com.smarthelpdesk.apigateway.exception;

import org.apache.coyote.BadRequestException;
/**
 * Ошибка некорректных входных данных.
 * Используется для нарушения бизнес-правил или валидации.
 */
public class ValidationException extends BaseException {
    public ValidationException(String message){
        super(message);
    }
}
