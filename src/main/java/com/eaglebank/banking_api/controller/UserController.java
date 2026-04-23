package com.eaglebank.banking_api.controller;

import com.eaglebank.banking_api.dto.request.CreateUserRequest;
import com.eaglebank.banking_api.dto.response.BadRequestErrorResponse;
import com.eaglebank.banking_api.dto.response.ErrorResponse;
import com.eaglebank.banking_api.dto.response.UserResponse;
import com.eaglebank.banking_api.entity.User;
import com.eaglebank.banking_api.mapper.user.UserRequestMapper;
import com.eaglebank.banking_api.mapper.user.UserResponseMapper;
import com.eaglebank.banking_api.service.UserService;
import com.eaglebank.banking_api.service.command.CreateUserCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/users")
@Tag(name = "user", description = "Manage a user")
public class UserController {
    private final UserService userService;
    private final UserRequestMapper userRequestMapper;
    private final UserResponseMapper userResponseMapper;

    public UserController(
            UserService userService, UserRequestMapper userRequestMapper, UserResponseMapper userResponseMapper) {
        this.userService = userService;
        this.userRequestMapper = userRequestMapper;
        this.userResponseMapper = userResponseMapper;
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @Operation(summary = "Create a new user", description = "Create a new user with the provided details")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "201",
                        description = "User has been created successfully",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = UserResponse.class))),
                @ApiResponse(
                        responseCode = "400",
                        description = "Invalid details supplied",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = BadRequestErrorResponse.class))),
                @ApiResponse(
                        responseCode = "500",
                        description = "An unexpected error occurred",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ErrorResponse.class)))
            })
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        CreateUserCommand command = userRequestMapper.toCommand(request);
        User newUser = userService.createUser(command);

        return userResponseMapper.toResponse(newUser);
    }
}
