package com.eaglebank.banking_api.service.command;

import com.eaglebank.banking_api.dto.AddressDto;

public record CreateUserCommand(String name, AddressDto address, String phoneNumber, String email) {}
