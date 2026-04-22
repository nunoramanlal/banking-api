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
        return new CreateUserCommand(
                request.name(),
                request.address().line1(),
                request.address().line2(),
                request.address().line3(),
                request.address().town(),
                request.address().county(),
                request.address().postcode(),
                request.phoneNumber(),
                request.email()
        );
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId().toString(),
                user.getName(),
                new AddressDto(
                        user.getLine1(),
                        user.getLine2(),
                        user.getLine3(),
                        user.getTown(),
                        user.getCounty(),
                        user.getPostcode()),
                user.getPhoneNumber(),
                user.getEmail(),
                user.getCreatedTimestamp().format(FORMATTER),
                user.getUpdatedTimestamp().format(FORMATTER));
    }
}
