package com.pm.bellavera.contact;

import com.pm.bellavera.contact.api.SendContactMessageRequest;
import com.pm.bellavera.user.AppUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sending a message to the admin inbox. The sender is whoever is signed in - never typed. */
@Service
public class ContactService {

    private final ContactMessageRepository contactMessageRepository;

    public ContactService(ContactMessageRepository contactMessageRepository) {
        this.contactMessageRepository = contactMessageRepository;
    }

    @Transactional
    public void send(AppUser user, SendContactMessageRequest request) {
        contactMessageRepository.save(ContactMessage.builder()
                .user(user)
                .subject(request.subject())
                .message(request.message())
                .build());
    }
}
