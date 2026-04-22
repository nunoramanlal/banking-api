package com.eaglebank.banking_api.dto.response;

import java.util.List;

public record BadRequestErrorResponse(String message, List<ValidationError> details) {}
