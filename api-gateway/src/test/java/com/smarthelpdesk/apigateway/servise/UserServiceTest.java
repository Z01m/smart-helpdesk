package com.smarthelpdesk.apigateway.servise;

import com.smarthelpdesk.apigateway.entity.User;
import com.smarthelpdesk.apigateway.exception.UserNotFoundException;
import com.smarthelpdesk.apigateway.repository.UserRepository;
import com.smarthelpdesk.apigateway.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User user;
    private UUID userId;
    private String email;

    @BeforeEach
    public void setUp() {
        userId = UUID.randomUUID();
        email = "Test@exaple.com";
        user = User.builder()
                .id(userId)
                .email(email)
                .fullName("Test user")
                .build();
    }

    @Test
    void getById_shouldReturnWhenUserExists() {
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        User result = userService.getById(userId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result).isEqualTo(user);
    }

    @Test
    void getById_shouldThrowWhenUserNotFound() {
        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(userId))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getByEmail_shouldReturnWhenUserExists() {
        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        User result = userService.getByEmail(email);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result).isEqualTo(user);
    }

    @Test
    void getByEmail_shouldThrowWhenUserNotFound() {
        when(userRepository.findByEmail(email))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getByEmail(email))
                .isInstanceOf(UserNotFoundException.class);
    }


}
