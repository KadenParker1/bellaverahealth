package com.pm.bellavera.store.api;

import com.pm.bellavera.store.ShippingAddress;

public record ShippingAddressDto(
        String name,
        String line1,
        String line2,
        String city,
        String region,
        String postalCode,
        String country) {

    public static ShippingAddressDto from(ShippingAddress address) {
        if (address == null || (address.getLine1() == null && address.getName() == null)) {
            return null;
        }
        return new ShippingAddressDto(address.getName(), address.getLine1(), address.getLine2(),
                address.getCity(), address.getRegion(), address.getPostalCode(), address.getCountry());
    }
}
