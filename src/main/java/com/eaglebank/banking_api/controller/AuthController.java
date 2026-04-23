package com.eaglebank.banking_api.controller;

import com.eaglebank.banking_api.dto.request.LoginRequest;
import com.eaglebank.banking_api.dto.request.RefreshTokenRequest;
import com.eaglebank.banking_api.dto.response.BadRequestErrorResponse;
import com.eaglebank.banking_api.dto.response.ErrorResponse;
import com.eaglebank.banking_api.dto.response.LoginResponse;
import com.eaglebank.banking_api.mapper.auth.AuthResponseMapper;
import com.eaglebank.banking_api.service.AuthService;
import com.eaglebank.banking_api.service.result.AuthResult;
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
@RequestMapping("/v1/auth")
@Tag(name = "auth", description = "Authentication")
public class AuthController {

    private final AuthService authService;
    private final AuthResponseMapper authResponseMapper;

    public AuthController(AuthService authService, AuthResponseMapper authResponseMapper) {
        this.authService = authService;
        this.authResponseMapper = authResponseMapper;
    }

    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/login")
    @Operation(summary = "Authenticate a user", description = "Exchange an email for access and refresh tokens")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Authentication successful",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = LoginResponse.class))),
                @ApiResponse(
                        responseCode = "400",
                        description = "Invalid details supplied",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = BadRequestErrorResponse.class))),
                @ApiResponse(
                        responseCode = "401",
                        description = "Invalid credentials",
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
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        AuthResult result = authService.login(request.email());
        return authResponseMapper.toLoginResponse(result);
    }

    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Exchange a refresh token for a new access token")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Token refreshed successfully",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = LoginResponse.class))),
                @ApiResponse(
                        responseCode = "401",
                        description = "Refresh token is invalid or expired",
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
    public LoginResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResult result = authService.refresh(request.refreshToken());
        return authResponseMapper.toLoginResponse(result);
    }
}
