package com.smarthelpdesk.notificationservice.mail;
import com.smarthelpdesk.notificationservice.security.AccessTokenDetails;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;
@RestController
@RequestMapping("/api/v1/notification-email")
@ConditionalOnProperty(name="notification.email.enabled",havingValue="true")
public class EmailController {
    private final EmailService service;
    public EmailController(EmailService service){this.service=service;}
    @GetMapping public EmailService.Status status(Authentication user){return service.status(UUID.fromString(user.getName()));}
    @PostMapping("/subscribe") @ResponseStatus(HttpStatus.ACCEPTED)
    public void subscribe(Authentication user){
        if(!(user.getDetails() instanceof AccessTokenDetails details))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        service.subscribe(UUID.fromString(user.getName()),details.email());
    }
    @PostMapping("/confirm") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirm(Authentication user,@RequestBody Confirmation request){service.confirm(UUID.fromString(user.getName()),request.token());}
    @DeleteMapping @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribe(Authentication user){service.unsubscribe(UUID.fromString(user.getName()));}
    @PostMapping("/retry-failed") @ResponseStatus(HttpStatus.ACCEPTED)
    public void retry(Authentication user){service.retryFailed(UUID.fromString(user.getName()));}
    public record Confirmation(String token){}
}
