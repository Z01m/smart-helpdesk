package com.smarthelpdesk.notificationservice.controller;

import com.smarthelpdesk.notificationservice.dto.NotificationResponse;
import com.smarthelpdesk.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService service;
    @GetMapping
    public Page<NotificationResponse> list(Principal user,
            @RequestParam(name = "unreadOnly", defaultValue = "false") boolean unreadOnly,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return service.list(UUID.fromString(user.getName()), unreadOnly, page, size);
    }
    @PostMapping("/{id}/delivered")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void acknowledge(Principal user, @PathVariable("id") UUID id) {
        service.acknowledge(UUID.fromString(user.getName()), id);
    }
    @PatchMapping("/{id}/read")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void markRead(Principal user, @PathVariable("id") UUID id) {
        service.markRead(UUID.fromString(user.getName()), id);
    }
}
