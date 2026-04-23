package com.eaglebank.banking_api.service;

import com.eaglebank.banking_api.entity.User;
import com.eaglebank.banking_api.mapper.user.UserEntityMapper;
import com.eaglebank.banking_api.repository.UserRepository;
import com.eaglebank.banking_api.service.command.CreateUserCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserEntityMapper userEntityMapper;

    public UserService(UserRepository userRepository, UserEntityMapper userEntityMapper) {
        this.userRepository = userRepository;
        this.userEntityMapper = userEntityMapper;
    }

    @Transactional
    public User createUser(CreateUserCommand command) {
        log.info("Creating user with email: {}", command.email());
        User savedUser = userRepository.save(userEntityMapper.toEntity(command));
        log.info("User created successfully with id: {}", savedUser.getId());
        return savedUser;
    }
}
