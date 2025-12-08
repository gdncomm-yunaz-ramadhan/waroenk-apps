package com.gdn.project.waroenk.cart.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Embedded document representing a snapshot of shipping address at checkout time.
 * This is immutable once created to preserve checkout data integrity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressSnapshot {
    private String recipientName;
    private String phone;
    private String street;
    private String city;
    private String province;
    private String district;
    private String subDistrict;
    private String country;
    private String postalCode;
    private String notes;
    private Float latitude;
    private Float longitude;
}






