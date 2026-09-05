package com.pm.bellavera.store;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A payment webhook we have already applied, keyed by the provider's own event id.
 *
 * <p>Stripe retries a webhook until it gets a 2xx, and can deliver the same event more than once
 * regardless. Inserting this row is what makes applying a payment idempotent: the primary-key
 * clash on a redelivery is the guard.
 */
@Entity
@Table(name = "payment_event", schema = "app")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class PaymentEvent {

    /** The provider's event id, e.g. Stripe's {@code evt_...}. */
    @Id
    private String id;

    @Column(nullable = false)
    private String type;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "received_at", nullable = false)
    @Builder.Default
    private Instant receivedAt = Instant.now();
}
