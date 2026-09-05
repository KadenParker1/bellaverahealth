package com.pm.bellavera.store;

/** How many units of a product are owed by orders that are paid but not yet shipped. */
public record CommittedQuantity(String productCode, Long quantity) {
}
