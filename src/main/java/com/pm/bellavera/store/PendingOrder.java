package com.pm.bellavera.store;

import com.pm.bellavera.store.payment.CheckoutLineItem;
import java.util.List;
import java.util.UUID;

/**
 * A committed PENDING order, plus the line items to send the payment provider.
 *
 * <p>The line items travel with it because the order's own {@code items} are lazy and the entity is
 * detached by the time the gateway is called - that call happens deliberately outside the
 * transaction, so there is no session left to load them from.
 */
public record PendingOrder(UUID orderId, String currency, String customerEmail,
                            List<CheckoutLineItem> lineItems) {
}
