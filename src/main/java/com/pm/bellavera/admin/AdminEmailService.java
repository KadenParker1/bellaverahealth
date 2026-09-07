package com.pm.bellavera.admin;

import com.pm.bellavera.admin.api.BroadcastEmailRequest;
import com.pm.bellavera.admin.api.BroadcastEmailResult;
import com.pm.bellavera.audit.AuditService;
import com.pm.bellavera.email.EmailGateway;
import com.pm.bellavera.email.EmailMessage;
import com.pm.bellavera.user.AppUser;
import com.pm.bellavera.user.AppUserRepository;
import com.pm.bellavera.user.UserStatus;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sends one email to every account that has opted into the email chain.
 *
 * <p>{@link EmailGateway} is a port, like the LLM and payment boundaries - under
 * {@code bellavera.email.provider=mock} (every profile today) nothing actually leaves the
 * process, which is what makes this exercisable offline.
 *
 * <p>A real provider will fail on some recipients (bad address, rate limit, transient network
 * error). Each send is isolated so one failure can't take out the rest of the run or the audit
 * row - the row records exactly what happened, including a partial send, rather than either
 * silently eating the failure or losing the record entirely.
 */
@Service
public class AdminEmailService {

    private static final Logger log = LoggerFactory.getLogger(AdminEmailService.class);

    static final String AUDIT_ACTION_BROADCAST = "EMAIL_BROADCAST_SENT";
    static final String AUDIT_ENTITY_BROADCAST = "email_broadcast";

    private final AppUserRepository appUserRepository;
    private final EmailGateway emailGateway;
    private final AuditService auditService;

    public AdminEmailService(AppUserRepository appUserRepository,
                              EmailGateway emailGateway,
                              AuditService auditService) {
        this.appUserRepository = appUserRepository;
        this.emailGateway = emailGateway;
        this.auditService = auditService;
    }

    @Transactional
    public BroadcastEmailResult broadcast(AppUser admin, BroadcastEmailRequest request) {
        List<AppUser> recipients = appUserRepository.findByStatusAndEmailOptInTrue(UserStatus.ACTIVE);

        int sent = 0;
        int failed = 0;
        for (AppUser user : recipients) {
            try {
                emailGateway.send(new EmailMessage(user.getEmail(), request.subject(), request.body()));
                sent++;
            } catch (RuntimeException e) {
                failed++;
                log.warn("Broadcast email failed for user {}: {}", user.getId(), e.getMessage());
            }
        }

        auditService.record(admin, AUDIT_ACTION_BROADCAST, AUDIT_ENTITY_BROADCAST, null,
                Map.of("subject", request.subject(), "sentCount", sent, "failedCount", failed));

        return new BroadcastEmailResult(sent, failed);
    }
}
