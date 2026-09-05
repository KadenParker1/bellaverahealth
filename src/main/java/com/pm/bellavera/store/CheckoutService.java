package com.pm.bellavera.store;

import com.pm.bellavera.common.ValidationException;
import com.pm.bellavera.store.api.CheckoutItemRequest;
import com.pm.bellavera.store.api.CheckoutRequest;
import com.pm.bellavera.store.api.CheckoutSessionDto;
import com.pm.bellavera.store.payment.CheckoutSession;
import com.pm.bellavera.store.payment.CheckoutSessionRequest;
import com.pm.bellavera.store.payment.PaymentGateway;
import com.pm.bellavera.user.AppUser;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Turns a basket of product codes into a PENDING order and a hosted checkout URL.
 *
 * <p>The client sends codes and quantities. Every price, the currency, and both redirect URLs are
 * resolved here - from {@code app.product} and {@link StoreProperties} respectively - so a
 * tampered request can change what is bought but never what it costs or where the browser lands.
 *
 * <p>The order is deliberately left PENDING. Only a signature-verified webhook marks it PAID; see
 * {@link PaymentApplicationService}.
 *
 * <p>Deliberately not transactional: the order must be committed <em>before</em> the payment
 * provider is told it exists, or the provider's webhook can arrive first and find nothing. The
 * commit boundaries live in {@link PendingOrderService}, one per step.
 */
@Service
public class CheckoutService {

    private final PendingOrderService pendingOrderService;
    private final PaymentGateway paymentGateway;
    private final StoreProperties storeProperties;

    public CheckoutService(PendingOrderService pendingOrderService,
                            PaymentGateway paymentGateway,
                            StoreProperties storeProperties) {
        this.pendingOrderService = pendingOrderService;
        this.paymentGateway = paymentGateway;
        this.storeProperties = storeProperties;
    }

    public CheckoutSessionDto startCheckout(AppUser user, CheckoutRequest request) {
        Map<String, Integer> quantitiesByCode = mergeQuantities(request.items());

        // Step 1 commits. From here the order is durable and findable by id, which is what the
        // webhook falls back to when it beats us to step 3.
        PendingOrder pending = pendingOrderService.createPending(user, quantitiesByCode);

        CheckoutSession session;
        try {
            session = paymentGateway.createCheckoutSession(new CheckoutSessionRequest(
                    pending.orderId(),
                    pending.customerEmail(),
                    pending.currency(),
                    pending.lineItems(),
                    appendOrderId(storeProperties.checkoutSuccessUrl(), pending.orderId().toString()),
                    storeProperties.checkoutCancelUrl()));
        } catch (RuntimeException ex) {
            pendingOrderService.abandon(pending.orderId());
            throw ex;
        }

        pendingOrderService.attachCheckoutSession(pending.orderId(), session.sessionId());
        return new CheckoutSessionDto(pending.orderId(), session.url());
    }

    /**
     * Collapses repeated codes into one line. The DB enforces one line per product per order, and
     * a client sending the same product twice means "two of them", not an error.
     */
    private Map<String, Integer> mergeQuantities(List<CheckoutItemRequest> items) {
        int maxQuantity = storeProperties.maxItemQuantityOrDefault();
        Map<String, Integer> merged = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        for (CheckoutItemRequest item : items) {
            int quantity = merged.merge(item.productCode(), item.quantity(), Integer::sum);
            if (quantity > maxQuantity) {
                errors.add("Quantity for '" + item.productCode() + "' exceeds the maximum of " + maxQuantity);
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors.stream().distinct().toList());
        }
        return merged;
    }

    private String appendOrderId(String url, String orderId) {
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("bellavera.store.checkout-success-url is not configured");
        }
        return url + (url.contains("?") ? "&" : "?") + "orderId=" + orderId;
    }
}
