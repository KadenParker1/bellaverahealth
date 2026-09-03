package com.pm.bellavera.user;

import com.pm.bellavera.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

/**
 * Mirrors a Supabase {@code auth.users} row. The id is never generated here - it is
 * assigned from the verified JWT {@code sub} claim the first time a user is seen.
 *
 * <p>Implements {@link Persistable} because the id is always non-null (assigned before save):
 * without it, Spring Data JPA assumes the row already exists and calls {@code merge()} instead of
 * {@code persist()} for a brand-new user, which both hits an extra SELECT and breaks cascading
 * into the equally-new {@link UserProfile} created in the same transaction.
 */
@Entity
@Table(name = "app_user", schema = "app")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id", callSuper = false)
public class AppUser extends AuditableEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Override
    @Transient
    public boolean isNew() {
        return getCreatedAt() == null;
    }
}
