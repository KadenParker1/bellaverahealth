package com.pm.bellavera.user;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    List<AppUser> findAllByOrderByEmailAsc();

    List<AppUser> findByStatusOrderByEmailAsc(UserStatus status);
}
