package com.eaglebank.banking_api.mapper;

import com.eaglebank.banking_api.dto.AddressDto;
import com.eaglebank.banking_api.dto.request.CreateUserRequest;
import com.eaglebank.banking_api.dto.response.UserResponse;
import com.eaglebank.banking_api.entity.User;
import com.eaglebank.banking_api.service.command.CreateUserCommand;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    public CreateUserCommand toCommand(CreateUserRequest request) {
        return new CreateUserCommand(request.name(), request.address(), request.phoneNumber(), request.email());
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId().toString(),
                user.getName(),
                new AddressDto(
                        user.getAddress().getLine1(),
                        user.getAddress().getLine2(),
                        user.getAddress().getLine3(),
                        user.getAddress().getTown(),
                        user.getAddress().getCounty(),
                        user.getAddress().getPostcode()),
                user.getPhoneNumber(),
                user.getEmail(),
                user.getCreatedTimestamp().format(FORMATTER),
                user.getUpdatedTimestamp().format(FORMATTER));
    }
}
