package com.eaglebank.banking_api.controller;

import com.eaglebank.banking_api.dto.request.CreateUserRequest;
import com.eaglebank.banking_api.dto.request.UpdateUserRequest;
import com.eaglebank.banking_api.dto.response.BadRequestErrorResponse;
import com.eaglebank.banking_api.dto.response.ErrorResponse;
import com.eaglebank.banking_api.dto.response.UserResponse;
import com.eaglebank.banking_api.entity.User;
import com.eaglebank.banking_api.mapper.user.UserRequestMapper;
import com.eaglebank.banking_api.mapper.user.UserResponseMapper;
import com.eaglebank.banking_api.service.UserService;
import com.eaglebank.banking_api.service.command.CreateUserCommand;
import com.eaglebank.banking_api.service.command.UpdateUserCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/users")
@Validated
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

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{userId}")
    @Operation(summary = "Fetch user by ID", description = "Fetch the details of a user")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "The user details",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = UserResponse.class))),
                @ApiResponse(
                        responseCode = "401",
                        description = "Access token is missing or invalid",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ErrorResponse.class))),
                @ApiResponse(
                        responseCode = "403",
                        description = "The user is not allowed to access this user",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ErrorResponse.class))),
                @ApiResponse(
                        responseCode = "404",
                        description = "User was not found",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ErrorResponse.class))),
                @ApiResponse(
                        responseCode = "500",
                        description = "An unexpected error occurred",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ErrorResponse.class)))
            })
    public UserResponse fetchUserById(
            @PathVariable @Pattern(regexp = "^usr-[A-Za-z0-9]+$", message = "User ID format is invalid")
                    String userId) {
        User user = userService.fetchUserById(userId);
        return userResponseMapper.toResponse(user);
    }

    @ResponseStatus(HttpStatus.OK)
    @PatchMapping("/{userId}")
    @Operation(summary = "Update user by ID", description = "Update the details of a user")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "The updated user details",
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
                        responseCode = "401",
                        description = "Access token is missing or invalid",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ErrorResponse.class))),
                @ApiResponse(
                        responseCode = "403",
                        description = "The user is not allowed to update this user",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ErrorResponse.class))),
                @ApiResponse(
                        responseCode = "404",
                        description = "User was not found",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ErrorResponse.class))),
                @ApiResponse(
                        responseCode = "500",
                        description = "An unexpected error occurred",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ErrorResponse.class)))
            })
    public UserResponse updateUser(
            @PathVariable @Pattern(regexp = "^usr-[A-Za-z0-9]+$", message = "User ID format is invalid") String userId,
            @Valid @RequestBody UpdateUserRequest request) {
        UpdateUserCommand command = userRequestMapper.toCommand(request);
        User updatedUser = userService.updateUser(userId, command);
        return userResponseMapper.toResponse(updatedUser);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete user by ID", description = "Delete a user account")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "204", description = "The user has been deleted"),
                    @ApiResponse(
                            responseCode = "400",
                            description = "The request didn't supply all the necessary data",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = BadRequestErrorResponse.class))),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Access token is missing or invalid",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(
                            responseCode = "403",
                            description = "The user is not allowed to access the transaction",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(
                            responseCode = "404",
                            description = "User was not found",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(
                            responseCode = "409",
                            description = "A user cannot be deleted when they are associated with a bank account",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(
                            responseCode = "500",
                            description = "An unexpected error occurred",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class)))
            })
    public void deleteUser(
            @PathVariable @Pattern(regexp = "^usr-[A-Za-z0-9]+$", message = "User ID format is invalid") String userId) {
        userService.deleteUser(userId);
    }
}
