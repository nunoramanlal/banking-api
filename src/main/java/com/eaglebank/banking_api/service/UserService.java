package com.eaglebank.banking_api.service;

import com.eaglebank.banking_api.entity.Address;
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
        Address address = new Address(
                command.address().line1(),
                command.address().line2(),
                command.address().line3(),
                command.address().town(),
                command.address().county(),
                command.address().postcode());

        User user = new User(command.name(), address, command.phoneNumber(), command.email());
        return userRepository.save(user);
    }
}
