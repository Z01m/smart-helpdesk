package com.smarthelpdesk.apigateway.repository;

import com.smarthelpdesk.apigateway.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
/**
 * Репозиторий для работы с пользователями.
 * Предоставляет поиск и сохранение пользователей в базе данных.
 */
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findById(UUID id);
    boolean existsByEmail(String email);
}
