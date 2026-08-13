package com.smarthelpdesk.apigateway.mapper;

import com.smarthelpdesk.apigateway.dto.response.TicketResponse;
import com.smarthelpdesk.apigateway.entity.Ticket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TicketMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "operatorId", source = "operator.id")
    TicketResponse toResponse(Ticket ticket);
}