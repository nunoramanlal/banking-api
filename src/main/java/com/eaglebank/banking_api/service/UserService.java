package com.eaglebank.banking_api.service;

import com.eaglebank.banking_api.entity.User;
import com.eaglebank.banking_api.repository.UserRepository;
import com.eaglebank.banking_api.service.command.CreateUserCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User createUser(CreateUserCommand command) {
        return userRepository.save(buildUser(command));
    }

    private User buildUser(CreateUserCommand command){
        return new User(
                command.name(),
                command.email(),
                command.phoneNumber(),
                command.line1(),
                command.line2(),
                command.line3(),
                command.town(),
                command.county(),
                command.postcode()
        );
    }
}
