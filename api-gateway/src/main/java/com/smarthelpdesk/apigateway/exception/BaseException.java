package com.smarthelpdesk.apigateway.exception;
/**
 * Базовое исключение приложения.
 * Используется как родитель для специализированных ошибок.
 */
public abstract class BaseException extends RuntimeException{
    public BaseException(String message){
        super(message);
    }
}
