package com.pm.bellavera.store;

import com.pm.bellavera.common.NotFoundException;
import com.pm.bellavera.store.api.ProductDto;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Storefront reads. Inactive products are invisible here - deactivation is how removal works. */
@Service
public class CatalogService {

    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    public CatalogService(ProductRepository productRepository, InventoryService inventoryService) {
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional(readOnly = true)
    public List<ProductDto> listActive() {
        List<Product> products = productRepository.findByActiveTrueOrderBySortOrderAscNameAsc();
        Map<String, Integer> available = inventoryService.availabilityByCode(products);
        return products.stream()
                .map(product -> ProductDto.from(product, available.get(product.getCode())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductDto getActiveByCode(String code) {
        Product product = productRepository.findByCode(code)
                .filter(Product::isActive)
                .orElseThrow(() -> new NotFoundException("Product not found: " + code));
        return ProductDto.from(product, inventoryService.availabilityOf(product));
    }
}
