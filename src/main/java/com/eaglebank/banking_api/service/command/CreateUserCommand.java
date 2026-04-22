package com.eaglebank.banking_api.service.command;

import com.eaglebank.banking_api.dto.AddressDto;

public record CreateUserCommand(
        String name,
        String line1,
        String line2,
        String line3,
        String town,
        String county,
        String postcode,
        String phoneNumber,
        String email
) {}