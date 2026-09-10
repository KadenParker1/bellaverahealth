package com.pm.bellavera.contact.api;

import com.pm.bellavera.contact.ContactService;
import com.pm.bellavera.user.AppUser;
import com.pm.bellavera.user.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Any signed-in user can send a message - like everything outside {@code /admin/**}. */
@RestController
@RequestMapping("/api/v1/contact")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void send(@CurrentUser AppUser user, @Valid @RequestBody SendContactMessageRequest request) {
        contactService.send(user, request);
    }
}
