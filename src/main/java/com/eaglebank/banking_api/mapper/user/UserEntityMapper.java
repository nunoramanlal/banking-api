package com.eaglebank.banking_api.mapper.user;

import com.eaglebank.banking_api.entity.User;
import com.eaglebank.banking_api.service.command.CreateUserCommand;
import com.eaglebank.banking_api.service.command.UpdateUserCommand;
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

    public void applyPatch(UpdateUserCommand command, User user) {
        if (command.name() != null) user.setName(command.name());
        if (command.email() != null) user.setEmail(command.email());
        if (command.phoneNumber() != null) user.setPhoneNumber(command.phoneNumber());
        if (command.line1() != null) user.setLine1(command.line1());
        if (command.line2() != null) user.setLine2(command.line2());
        if (command.line3() != null) user.setLine3(command.line3());
        if (command.town() != null) user.setTown(command.town());
        if (command.county() != null) user.setCounty(command.county());
        if (command.postcode() != null) user.setPostcode(command.postcode());
    }
}
