package com.pm.bellavera.admin.api;

import com.pm.bellavera.admin.AdminProductService;
import com.pm.bellavera.user.AppUser;
import com.pm.bellavera.user.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/products")
public class AdminProductController {

    private final AdminProductService adminProductService;

    public AdminProductController(AdminProductService adminProductService) {
        this.adminProductService = adminProductService;
    }

    @GetMapping
    public List<AdminProductDto> list() {
        return adminProductService.list();
    }

    @GetMapping("/{productId}")
    public AdminProductDto get(@PathVariable UUID productId) {
        return adminProductService.get(productId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminProductDto create(@CurrentUser AppUser admin, @Valid @RequestBody CreateProductRequest request) {
        return adminProductService.create(admin, request);
    }

    @PatchMapping("/{productId}")
    public AdminProductDto update(@CurrentUser AppUser admin, @PathVariable UUID productId,
                                   @Valid @RequestBody UpdateProductRequest request) {
        return adminProductService.update(admin, productId, request);
    }

    /**
     * Removes a product from the storefront by deactivating it. Mapped to DELETE because that is
     * the gesture the admin UI offers, but no row is destroyed - order lines still point here.
     */
    @DeleteMapping("/{productId}")
    public AdminProductDto deactivate(@CurrentUser AppUser admin, @PathVariable UUID productId) {
        return adminProductService.deactivate(admin, productId);
    }
}
