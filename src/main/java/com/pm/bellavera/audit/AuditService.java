package com.pm.bellavera.audit;

import com.pm.bellavera.user.AppUser;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes the trail for administrative mutations. Every write behind {@code /api/v1/admin/**}
 * that changes something calls this.
 *
 * <p>The record joins the caller's transaction on purpose: an audit row for a change that rolled
 * back would be a lie.
 */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(AppUser actor, String action, String entityType, UUID entityId,
                        Map<String, Object> before, Map<String, Object> after) {
        auditLogRepository.save(AuditLog.builder()
                .actorUserId(actor == null ? null : actor.getId())
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .beforeState(before)
                .afterState(after)
                .build());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(AppUser actor, String action, String entityType, UUID entityId, Map<String, Object> after) {
        record(actor, action, entityType, entityId, null, after);
    }
}
