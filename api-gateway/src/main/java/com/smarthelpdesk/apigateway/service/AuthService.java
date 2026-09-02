package com.smarthelpdesk.apigateway.service;


import com.smarthelpdesk.apigateway.dto.response.AuthResponse;
import com.smarthelpdesk.apigateway.dto.response.RegisterResponse;
import com.smarthelpdesk.apigateway.entity.User;
import com.smarthelpdesk.apigateway.entity.enums.CustomerTier;
import com.smarthelpdesk.apigateway.entity.enums.Role;
import com.smarthelpdesk.apigateway.exception.EmailAlreadyExistsException;
import com.smarthelpdesk.apigateway.exception.InvalidCredentialsException;
import com.smarthelpdesk.apigateway.repository.UserRepository;
import com.smarthelpdesk.apigateway.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public RegisterResponse register(String email, String rawPassword, String fullName)  {
         if(userRepository.existsByEmail(email)) {
             throw new EmailAlreadyExistsException(email);
         }
         String hashPassword =  passwordEncoder.encode(rawPassword);

         User user = new User();
         user.setEmail(email);
         user.setFullName(fullName);
         user.setPasswordHash(hashPassword);
         user.setRole(Role.CUSTOMER);
         user.setCustomerTier(CustomerTier.STANDARD);
         User savedUser = userRepository.save(user);
         RegisterResponse response = new RegisterResponse(
                 savedUser.getId(),
                 savedUser.getEmail(),
                 savedUser.getFullName(),
                 savedUser.getRole(),
                 savedUser.getCustomerTier()
         );
         return response;
    }
    @Transactional
    public AuthResponse login(String email, String rawPassword)  {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new InvalidCredentialsException());

        if(!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(),email,user.getRole().name());

        RefreshTokenService.CreatedRefreshToken createdRefreshToken = refreshTokenService.create(user);
        String refreshToken = createdRefreshToken.rawToken();

        long expiresIn = 900000L;

        return new AuthResponse(
                accessToken,
                refreshToken,
                expiresIn
        );
    }
    @Transactional
    public AuthResponse refresh(
            String oldRefreshToken
    )  {

        RefreshTokenService.CreatedRefreshToken newRefreshToken = refreshTokenService.rotateToken(oldRefreshToken);

        User user = newRefreshToken.entity().getUser();

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());

        return new AuthResponse(
                accessToken,
                newRefreshToken.rawToken(),
                900000L
        );
    }
    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }
}
