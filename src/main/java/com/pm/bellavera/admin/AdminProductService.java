package com.pm.bellavera.admin;

import com.pm.bellavera.admin.api.AdminProductDto;
import com.pm.bellavera.admin.api.CreateProductRequest;
import com.pm.bellavera.admin.api.UpdateProductRequest;
import com.pm.bellavera.audit.AuditService;
import com.pm.bellavera.common.NotFoundException;
import com.pm.bellavera.common.ValidationException;
import com.pm.bellavera.store.InventoryService;
import com.pm.bellavera.store.Product;
import com.pm.bellavera.store.ProductRepository;
import com.pm.bellavera.store.StoreProperties;
import com.pm.bellavera.user.AppUser;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Catalog editing. Like surveys, removal is deactivation: {@code order_item} rows reference a
 * product, and a deleted product would break the history of what someone actually bought.
 */
@Service
public class AdminProductService {

    static final String AUDIT_ENTITY_PRODUCT = "product";

    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final StoreProperties storeProperties;
    private final AuditService auditService;

    public AdminProductService(ProductRepository productRepository, InventoryService inventoryService,
                                StoreProperties storeProperties, AuditService auditService) {
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
        this.storeProperties = storeProperties;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<AdminProductDto> list() {
        List<Product> products = productRepository.findAllByOrderBySortOrderAscNameAsc();
        Map<String, Integer> available = inventoryService.availabilityByCode(products);
        return products.stream()
                .map(product -> AdminProductDto.from(product, available.get(product.getCode())))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminProductDto get(UUID productId) {
        return withAvailability(findProduct(productId));
    }

    @Transactional
    public AdminProductDto create(AppUser admin, CreateProductRequest request) {
        if (productRepository.existsByCode(request.code())) {
            throw new ValidationException("A product with code '" + request.code() + "' already exists");
        }

        Product product = productRepository.saveAndFlush(Product.builder()
                .code(request.code())
                .name(request.name())
                .description(request.description())
                .imageUrl(request.imageUrl())
                .priceCents(request.priceCents())
                .currency(normalizeCurrency(request.currency()))
                .stripePriceId(blankToNull(request.stripePriceId()))
                .active(true)
                .sortOrder(request.sortOrder() == null ? 0 : request.sortOrder())
                .stockQuantity(request.stockQuantity())
                .build());

        auditService.record(admin, "PRODUCT_CREATED", AUDIT_ENTITY_PRODUCT, product.getId(), Map.of(
                "code", product.getCode(),
                "name", product.getName(),
                "priceCents", product.getPriceCents()));

        return withAvailability(product);
    }

    @Transactional
    public AdminProductDto update(AppUser admin, UUID productId, UpdateProductRequest request) {
        Product product = findProduct(productId);

        Map<String, Object> before = snapshot(product);

        if (request.name() != null) {
            product.setName(request.name());
        }
        if (request.description() != null) {
            product.setDescription(request.description());
        }
        if (request.imageUrl() != null) {
            product.setImageUrl(blankToNull(request.imageUrl()));
        }
        if (request.priceCents() != null) {
            product.setPriceCents(request.priceCents());
        }
        if (request.currency() != null) {
            product.setCurrency(normalizeCurrency(request.currency()));
        }
        if (request.stripePriceId() != null) {
            product.setStripePriceId(blankToNull(request.stripePriceId()));
        }
        if (request.active() != null) {
            product.setActive(request.active());
        }
        if (request.sortOrder() != null) {
            product.setSortOrder(request.sortOrder());
        }
        // Null stockQuantity means "unchanged", so stopping tracking needs its own explicit flag.
        if (Boolean.TRUE.equals(request.clearStock())) {
            product.setStockQuantity(null);
        } else if (request.stockQuantity() != null) {
            product.setStockQuantity(request.stockQuantity());
        }
        productRepository.saveAndFlush(product);

        auditService.record(admin, "PRODUCT_UPDATED", AUDIT_ENTITY_PRODUCT, product.getId(),
                before, snapshot(product));

        return withAvailability(product);
    }

    /** Takes a product off the storefront. Deliberately not a delete - see the class comment. */
    @Transactional
    public AdminProductDto deactivate(AppUser admin, UUID productId) {
        Product product = findProduct(productId);
        if (!product.isActive()) {
            return withAvailability(product);
        }
        product.setActive(false);
        productRepository.saveAndFlush(product);

        auditService.record(admin, "PRODUCT_DEACTIVATED", AUDIT_ENTITY_PRODUCT, product.getId(),
                Map.of("active", true), Map.of("active", false));

        return withAvailability(product);
    }

    private AdminProductDto withAvailability(Product product) {
        return AdminProductDto.from(product, inventoryService.availabilityOf(product));
    }

    private Map<String, Object> snapshot(Product product) {
        Map<String, Object> state = new HashMap<>();
        state.put("name", product.getName());
        state.put("description", product.getDescription());
        state.put("priceCents", product.getPriceCents());
        state.put("currency", product.getCurrency());
        state.put("stripePriceId", product.getStripePriceId());
        state.put("active", product.isActive());
        state.put("sortOrder", product.getSortOrder());
        state.put("stockQuantity", product.getStockQuantity());
        return state;
    }

    private String normalizeCurrency(String currency) {
        return currency == null || currency.isBlank()
                ? storeProperties.currencyOrDefault()
                : currency.trim().toLowerCase();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private Product findProduct(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
    }
}
