package com.pm.bellavera.store;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findByActiveTrueOrderBySortOrderAscNameAsc();

    List<Product> findAllByOrderBySortOrderAscNameAsc();

    Optional<Product> findByCode(String code);

    List<Product> findByCodeIn(List<String> codes);

    boolean existsByCode(String code);
}
