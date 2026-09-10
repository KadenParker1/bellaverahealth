package com.pm.bellavera.admin;

import com.pm.bellavera.admin.api.AdminContactMessageDto;
import com.pm.bellavera.audit.AuditService;
import com.pm.bellavera.common.NotFoundException;
import com.pm.bellavera.common.PageResponse;
import com.pm.bellavera.contact.ContactMessage;
import com.pm.bellavera.contact.ContactMessageRepository;
import com.pm.bellavera.user.AppUser;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The admin inbox for messages sent through the Contact page. */
@Service
public class AdminContactService {

    static final String AUDIT_ENTITY_CONTACT_MESSAGE = "contact_message";

    private final ContactMessageRepository contactMessageRepository;
    private final AuditService auditService;

    public AdminContactService(ContactMessageRepository contactMessageRepository, AuditService auditService) {
        this.contactMessageRepository = contactMessageRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminContactMessageDto> list(Pageable pageable) {
        return PageResponse.of(contactMessageRepository
                .findAllByOrderByCreatedAtDesc(pageable)
                .map(AdminContactMessageDto::from));
    }

    @Transactional
    public AdminContactMessageDto markRead(AppUser admin, UUID messageId) {
        ContactMessage message = contactMessageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("Message not found"));
        if (message.getReadAt() == null) {
            message.setReadAt(Instant.now());
            contactMessageRepository.saveAndFlush(message);
            auditService.record(admin, "CONTACT_MESSAGE_READ", AUDIT_ENTITY_CONTACT_MESSAGE, message.getId(),
                    Map.of("readAt", message.getReadAt().toString()));
        }
        return AdminContactMessageDto.from(message);
    }
}
