package com.unicornt.store.domain.model;

/**
 * Where a confirmed order ships. Plain-Java value object: {@code street}, {@code city}
 * and {@code region} are required (PLAN.md section 2.4); {@code zipCode} is optional
 * and normalised to {@code null} when blank. Validated on construction, so an order
 * can never carry an incomplete address.
 */
public record ShippingAddress(String street, String city, String region, String zipCode) {

    public ShippingAddress {
        street = require(street, "street");
        city = require(city, "city");
        region = require(region, "region");
        zipCode = zipCode == null || zipCode.isBlank() ? null : zipCode.trim();
    }

    private static String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    /** Single-line rendering kept as a redundant snapshot next to the structured fields. */
    public String oneLine() {
        String base = street + ", " + city + ", " + region;
        return zipCode == null ? base : base + " " + zipCode;
    }
}
