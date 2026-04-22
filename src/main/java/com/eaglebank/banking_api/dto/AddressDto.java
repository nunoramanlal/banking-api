package com.eaglebank.banking_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record AddressDto(
        @Schema(
                description = "User address line 1",
                example = "Test address 1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Address line 1 is required")
        String line1,

        @Schema(description = "User address line 2", example = "Test address 2")
        String line2,

        @Schema(description = "User address line 3", example = "Test address 3")
        String line3,

        @Schema(description = "User town", example = "Town", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Town is required")
        String town,

        @Schema(description = "User County", example = "County", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "County is required")
        String county,

        @Schema(description = "PT1 1CD", example = "Postcode", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Postcode is required")
        String postcode) {}
