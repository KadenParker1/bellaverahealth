package com.pm.bellavera.user;

import com.pm.bellavera.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

/**
 * Health-neutral profile data. Medical / survey-derived facts live in {@code answer}, not here.
 *
 * <p>Implements {@link Persistable} for the same reason as {@link AppUser}: the id (shared with
 * the owning user) is always non-null, so without it Spring Data would call {@code merge()} on a
 * brand-new profile instead of {@code persist()}.
 */
@Entity
@Table(name = "user_profile", schema = "app")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "userId", callSuper = false)
public class UserProfile extends AuditableEntity implements Persistable<UUID> {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "birth_year")
    private Integer birthYear;

    private String country;

    private String timezone;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_system", nullable = false)
    private UnitSystem unitSystem;

    @Column(name = "onboarding_completed_at")
    private Instant onboardingCompletedAt;

    @Column(name = "consent_terms_at")
    private Instant consentTermsAt;

    @Column(name = "consent_ai_at")
    private Instant consentAiAt;

    @Override
    @Transient
    public UUID getId() {
        return userId;
    }

    @Override
    @Transient
    public boolean isNew() {
        return getCreatedAt() == null;
    }
}
