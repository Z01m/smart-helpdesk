package com.smarthelpdesk.apigateway.mapper;

import com.smarthelpdesk.apigateway.dto.response.TicketResponse;
import com.smarthelpdesk.apigateway.entity.Ticket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
/**
 * Преобразует Ticket между Entity и DTO.
 * Позволяет не передавать JPA-сущности напрямую через REST API.
 */
@Mapper(componentModel = "spring")
public interface TicketMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "operatorId", source = "operator.id")
    @Mapping(target = "generatedAnswer", ignore = true) // появится на этапе AI-worker
    TicketResponse toResponse(Ticket ticket);
}