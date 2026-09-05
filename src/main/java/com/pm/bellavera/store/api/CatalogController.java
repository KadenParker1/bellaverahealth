package com.pm.bellavera.store.api;

import com.pm.bellavera.store.CatalogService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The storefront. Browsing does not require an account; buying does. */
@RestController
@RequestMapping("/api/v1/store/products")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public List<ProductDto> list() {
        return catalogService.listActive();
    }

    @GetMapping("/{code}")
    public ProductDto get(@PathVariable String code) {
        return catalogService.getActiveByCode(code);
    }
}
