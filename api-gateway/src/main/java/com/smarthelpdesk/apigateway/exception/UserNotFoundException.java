package com.smarthelpdesk.apigateway.exception;

import java.util.UUID;
/**
 * Ошибка, возникающая при отсутствии пользователя.
 * Обычно приводит к HTTP 404 Not Found.
 */
public class UserNotFoundException extends NotFoundException{
    public UserNotFoundException(UUID userId){
        super("user not founded" + userId);
    }
    public UserNotFoundException(String email){
        super("user not founded" + email);
    }
}
