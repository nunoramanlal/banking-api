package com.eaglebank.banking_api.service;

import com.eaglebank.banking_api.entity.User;
import com.eaglebank.banking_api.exception.ForbiddenException;
import com.eaglebank.banking_api.exception.NotFoundException;
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

    @Transactional(readOnly = true)
    public User fetchUserById(String userId, String authenticatedUserId) {
        log.info("Fetching user: {} by authenticated user: {}", userId, authenticatedUserId);

        if (!userId.equals(authenticatedUserId)) {
            throw new ForbiddenException("You are not allowed to access this user");
        }

        return userRepository
                .findById(userId)
                .orElseThrow(() -> new NotFoundException("User was not found"));
    }
}
