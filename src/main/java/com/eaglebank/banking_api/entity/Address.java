package com.eaglebank.banking_api.entity;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Embeddable
public class Address {

    private String line1;
    private String line2;
    private String line3;
    private String town;
    private String county;
    private String postcode;

    public Address(String line1, String line2, String line3, String town, String county, String postcode) {
        this.line1 = line1;
        this.line2 = line2;
        this.line3 = line3;
        this.town = town;
        this.county = county;
        this.postcode = postcode;
    }
}
