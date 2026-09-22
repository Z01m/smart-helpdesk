package com.smarthelpdesk.aiworker.repository;

import com.smarthelpdesk.aiworker.entity.AiProcessingResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiProcessingResultRepository
        extends JpaRepository<AiProcessingResult, UUID> {
}