package com.pm.bellavera.store;

import com.pm.bellavera.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A sellable item. Money lives here in minor units ({@code price_cents}) and is read server-side
 * at checkout - a checkout request names products and quantities, never prices.
 *
 * <p>{@code stripePriceId} is optional: when set, checkout hands Stripe the pre-registered price
 * instead of an inline amount, which is what you want once the catalog is mirrored into Stripe.
 */
@Entity
@Table(name = "product", schema = "app")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id", callSuper = false)
public class Product extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "price_cents", nullable = false)
    private int priceCents;

    @Column(nullable = false)
    @Builder.Default
    private String currency = "usd";

    @Column(name = "stripe_price_id")
    private String stripePriceId;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    /**
     * Units on hand, or {@code null} when this product is not stock-tracked. Decremented when an
     * order containing it is fulfilled, not when it is bought - see {@code FulfillmentService}.
     */
    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    public boolean isStockTracked() {
        return stockQuantity != null;
    }
}
