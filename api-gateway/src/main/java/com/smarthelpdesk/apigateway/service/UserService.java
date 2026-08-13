package com.smarthelpdesk.apigateway.service;

import com.smarthelpdesk.apigateway.entity.User;
import com.smarthelpdesk.apigateway.exception.UserNotFoundException;
import com.smarthelpdesk.apigateway.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class UserService {

    private UserRepository userRepository;
    @Transactional(readOnly = true)
    public User getById(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(()-> new UserNotFoundException(userId));

        return user;
    }

    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(()-> new UserNotFoundException(email));
        return user;
    }

    public void updateProfile(UUID userId)
    {

    }

}
