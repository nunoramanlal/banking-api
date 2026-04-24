package com.eaglebank.banking_api.service;

import com.eaglebank.banking_api.entity.User;
import com.eaglebank.banking_api.exception.NotFoundException;
import com.eaglebank.banking_api.mapper.user.UserEntityMapper;
import com.eaglebank.banking_api.repository.UserRepository;
import com.eaglebank.banking_api.service.command.CreateUserCommand;
import com.eaglebank.banking_api.service.command.UpdateUserCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @PreAuthorize("#userId == authentication.principal")
    @Transactional(readOnly = true)
    public User fetchUserById(String userId) {
        log.info("Fetching user: {}", userId);

        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User was not found"));
    }

    @Transactional
    @PreAuthorize("#userId == authentication.principal")
    public User updateUser(String userId, UpdateUserCommand command) {
        log.info("Updating user: {}", userId);

        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User was not found"));

        userEntityMapper.applyPatch(command, user);

        return userRepository.save(user);
    }
}
