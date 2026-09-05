package com.pm.bellavera.store;

import com.pm.bellavera.common.NotFoundException;
import com.pm.bellavera.common.ValidationException;
import com.pm.bellavera.store.payment.CheckoutLineItem;
import com.pm.bellavera.user.AppUser;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The transactional steps of checkout, split out so each one commits on its own.
 *
 * <p>This split is the point of the class. Creating the order and calling the payment provider used
 * to share one transaction, which meant the order row did not exist for anybody else until after
 * the provider had already been told about it - and the provider can call the webhook back within
 * that window. A webhook that lands then finds no order by session id (not yet set) and none by id
 * (not yet committed), and the payment is lost. Committing the order first closes the window: by
 * the time the session exists, the order it names is durable and visible.
 */
@Service
public class PendingOrderService {

    private static final Logger log = LoggerFactory.getLogger(PendingOrderService.class);

    private final ProductRepository productRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final InventoryService inventoryService;
    private final StoreProperties storeProperties;

    public PendingOrderService(ProductRepository productRepository,
                                CustomerOrderRepository customerOrderRepository,
                                InventoryService inventoryService,
                                StoreProperties storeProperties) {
        this.productRepository = productRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.inventoryService = inventoryService;
        this.storeProperties = storeProperties;
    }

    /** Validates the basket and commits a PENDING order for it. */
    @Transactional
    public PendingOrder createPending(AppUser user, Map<String, Integer> quantitiesByCode) {
        List<Product> products = productRepository.findByCodeIn(List.copyOf(quantitiesByCode.keySet()));
        validateProducts(quantitiesByCode.keySet(), products);
        validateAvailability(quantitiesByCode, products);

        Map<String, Product> productsByCode = products.stream()
                .collect(Collectors.toMap(Product::getCode, Function.identity()));

        CustomerOrder order = CustomerOrder.builder()
                .user(user)
                .status(OrderStatus.PENDING)
                .currency(storeProperties.currencyOrDefault())
                .email(user.getEmail())
                .placedAt(Instant.now())
                .items(new ArrayList<>())
                .build();

        int subtotal = 0;
        List<CheckoutLineItem> lineItems = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : quantitiesByCode.entrySet()) {
            Product product = productsByCode.get(entry.getKey());
            int quantity = entry.getValue();
            int lineTotal = product.getPriceCents() * quantity;
            subtotal += lineTotal;

            order.addItem(OrderItem.builder()
                    .product(product)
                    .productCode(product.getCode())
                    .productName(product.getName())
                    .unitPriceCents(product.getPriceCents())
                    .quantity(quantity)
                    .lineTotalCents(lineTotal)
                    .build());

            lineItems.add(new CheckoutLineItem(product.getCode(), product.getName(), product.getDescription(),
                    product.getImageUrl(), product.getPriceCents(), quantity, product.getStripePriceId()));
        }
        order.setSubtotalCents(subtotal);

        CustomerOrder saved = customerOrderRepository.saveAndFlush(order);
        return new PendingOrder(saved.getId(), saved.getCurrency(), saved.getEmail(), lineItems);
    }

    /** Records which provider session belongs to this order, so its webhook can be matched. */
    @Transactional
    public void attachCheckoutSession(UUID orderId, String sessionId) {
        CustomerOrder order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        order.setStripeCheckoutSessionId(sessionId);
    }

    /**
     * Stands down an order whose checkout session could not be created. Without this the order
     * would sit PENDING forever: no session exists, so no expiry event will ever arrive for it.
     */
    @Transactional
    public void abandon(UUID orderId) {
        customerOrderRepository.findById(orderId).ifPresent(order -> {
            if (order.getStatus() == OrderStatus.PENDING) {
                order.setStatus(OrderStatus.CANCELLED);
                log.info("Cancelled order {} because its checkout session could not be created", orderId);
            }
        });
    }

    private void validateProducts(Set<String> requestedCodes, List<Product> found) {
        Map<String, Product> byCode = found.stream()
                .collect(Collectors.toMap(Product::getCode, Function.identity()));
        String currency = storeProperties.currencyOrDefault();

        List<String> errors = new ArrayList<>();
        for (String code : requestedCodes) {
            Product product = byCode.get(code);
            if (product == null) {
                errors.add("Unknown product: " + code);
            } else if (!product.isActive()) {
                errors.add("Product is no longer available: " + code);
            } else if (!currency.equalsIgnoreCase(product.getCurrency())) {
                errors.add("Product '" + code + "' is priced in " + product.getCurrency()
                        + " but the store checks out in " + currency);
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    /**
     * Refuses a basket the catalog cannot cover. Availability already nets off paid-but-unshipped
     * orders, so this closes the window between paying and packing. Two people checking out the
     * last unit at the same instant can still both get through - stock is only committed once a
     * payment lands - which is the usual trade for a shop that does not hold inventory in a cart.
     */
    private void validateAvailability(Map<String, Integer> quantitiesByCode, List<Product> products) {
        Map<String, Integer> available = inventoryService.availabilityByCode(products);
        List<String> errors = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : quantitiesByCode.entrySet()) {
            Integer stock = available.get(entry.getKey());
            if (stock == null) {
                continue; // not stock-tracked
            }
            if (stock <= 0) {
                errors.add("Out of stock: " + entry.getKey());
            } else if (entry.getValue() > stock) {
                errors.add("Only " + stock + " left of '" + entry.getKey() + "'");
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
