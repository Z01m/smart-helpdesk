package com.smarthelpdesk.apigateway.exception;
/**
 * Базовая ошибка отсутствующего ресурса.
 * Используется как родитель для ошибок вида "объект не найден".
 */
public class NotFoundException extends BaseException{
    public NotFoundException(String message){
        super(message);
    }
}
