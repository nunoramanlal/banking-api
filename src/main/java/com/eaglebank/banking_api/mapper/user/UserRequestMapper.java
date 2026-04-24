package com.eaglebank.banking_api.mapper.user;

import com.eaglebank.banking_api.dto.request.CreateUserRequest;
import com.eaglebank.banking_api.dto.request.UpdateUserRequest;
import com.eaglebank.banking_api.service.command.CreateUserCommand;
import com.eaglebank.banking_api.service.command.UpdateUserCommand;
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

    public UpdateUserCommand toCommand(UpdateUserRequest request) {
        return new UpdateUserCommand(
                request.name(),
                request.address() == null ? null : request.address().line1(),
                request.address() == null ? null : request.address().line2(),
                request.address() == null ? null : request.address().line3(),
                request.address() == null ? null : request.address().town(),
                request.address() == null ? null : request.address().county(),
                request.address() == null ? null : request.address().postcode(),
                request.phoneNumber(),
                request.email());
    }
}
