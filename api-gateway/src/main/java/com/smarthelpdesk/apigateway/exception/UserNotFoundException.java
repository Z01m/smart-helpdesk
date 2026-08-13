package com.smarthelpdesk.apigateway.exception;

import java.util.UUID;

public class UserNotFoundException extends NotFoundException{
    public UserNotFoundException(UUID userId){
        super("user not founded" + userId);
    }
    public UserNotFoundException(String email){
        super("user not founded" + email);
    }
}
