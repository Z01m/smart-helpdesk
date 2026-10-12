package com.smarthelpdesk.apigateway.repository;

import com.smarthelpdesk.apigateway.entity.RefreshToken;
import com.smarthelpdesk.apigateway.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select t from RefreshToken t where t.tokenHash = :hash")
    Optional<RefreshToken> findForRotation(@org.springframework.data.repository.query.Param("hash") String hash);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findAllByTokenHash(String tokenHash);

    List<RefreshToken> findAllByUser(User user);

    void deleteByUser(User user);
}
