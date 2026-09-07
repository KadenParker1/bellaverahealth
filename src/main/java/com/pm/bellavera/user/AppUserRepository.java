package com.pm.bellavera.user;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    List<AppUser> findAllByOrderByEmailAsc();

    List<AppUser> findByStatusOrderByEmailAsc(UserStatus status);

    @Query("select u from AppUser u join UserProfile p on p.userId = u.id "
            + "where u.status = :status and p.emailOptIn = true")
    List<AppUser> findByStatusAndEmailOptInTrue(@Param("status") UserStatus status);
}
