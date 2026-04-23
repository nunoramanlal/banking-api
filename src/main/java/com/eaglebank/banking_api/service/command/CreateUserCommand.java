package com.eaglebank.banking_api.service.command;

public record CreateUserCommand(
        String name,
        String line1,
        String line2,
        String line3,
        String town,
        String county,
        String postcode,
        String phoneNumber,
        String email) {}
