package com.smarthelpdesk.apigateway.repository;

import com.smarthelpdesk.apigateway.entity.RefreshToken;
import com.smarthelpdesk.apigateway.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findAllByTokenHash(String tokenHash);

    List<RefreshToken> findAllByUser(User user);

    void deleteByUser(User user);
}
