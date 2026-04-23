package com.eaglebank.banking_api.mapper.user;

import com.eaglebank.banking_api.entity.User;
import com.eaglebank.banking_api.service.command.CreateUserCommand;
import org.springframework.stereotype.Component;

@Component
public class UserEntityMapper {

    public User toEntity(CreateUserCommand command) {
        return new User(
                command.name(),
                command.email(),
                command.phoneNumber(),
                command.line1(),
                command.line2(),
                command.line3(),
                command.town(),
                command.county(),
                command.postcode());
    }
}
