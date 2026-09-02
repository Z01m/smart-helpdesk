package com.smarthelpdesk.apigateway.service;

import com.smarthelpdesk.apigateway.entity.RefreshToken;
import com.smarthelpdesk.apigateway.entity.User;
import com.smarthelpdesk.apigateway.exception.InvalidRefreshTokenException;
import com.smarthelpdesk.apigateway.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    public record CreatedRefreshToken(
            RefreshToken entity,
            String rawToken
    ) {
    }

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public CreatedRefreshToken create(User user){

        String rawToken = generateToken();
        String tokenHash = hashToken(rawToken);

        Instant expiresAt = Instant.now()
                .plus(14, ChronoUnit.DAYS);

        RefreshToken entity = new RefreshToken(user,
                tokenHash,
                expiresAt
        );

        refreshTokenRepository.save(entity);

        return new CreatedRefreshToken(entity, rawToken);
    }

    private String hashToken(String rawTocken) {

        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(rawTocken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        }
        catch (NoSuchAlgorithmException e){
            throw new RuntimeException(e);
        }
    }

    public String generateToken(){
        byte[] randomToken = new byte[64];

        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(randomToken);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomToken);

    }
    @Transactional(readOnly = true)
    public RefreshToken validate(String rawToken) {
        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHash(hashToken(rawToken))
                .orElseThrow(() -> new InvalidRefreshTokenException("token not found"));

        if(Boolean.TRUE.equals(refreshToken.getRevoked())){
            throw new InvalidRefreshTokenException("token is revoked");
        }
        if(refreshToken.getExpiresAt().isBefore(Instant.now())){
            throw new InvalidRefreshTokenException("token is expired");
        }
        return refreshToken;
    }
    @Transactional
    public void revoke(String rawToken){
        RefreshToken refreshToken = validate(rawToken);
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }
    @Transactional
    public CreatedRefreshToken rotateToken(String oldRawToken){
        RefreshToken refreshToken = validate(oldRawToken);
        User user = refreshToken.getUser();
        revoke(oldRawToken);
        return create(user);
    }



}
