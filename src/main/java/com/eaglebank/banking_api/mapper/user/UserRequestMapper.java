package com.eaglebank.banking_api.mapper.user;

import com.eaglebank.banking_api.dto.request.CreateUserRequest;
import com.eaglebank.banking_api.service.command.CreateUserCommand;
import org.springframework.stereotype.Component;

@Component
public class UserRequestMapper {

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
                request.email());
    }
}
