package com.pm.bellavera.store;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Where the parcel goes. Populated from the payment provider's collected shipping details. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingAddress {

    @Column(name = "ship_to_name")
    private String name;

    @Column(name = "ship_to_line1")
    private String line1;

    @Column(name = "ship_to_line2")
    private String line2;

    @Column(name = "ship_to_city")
    private String city;

    @Column(name = "ship_to_region")
    private String region;

    @Column(name = "ship_to_postal_code")
    private String postalCode;

    @Column(name = "ship_to_country")
    private String country;
}
