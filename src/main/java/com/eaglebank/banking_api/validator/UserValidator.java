package com.eaglebank.banking_api.validator;

import com.eaglebank.banking_api.dto.AddressDto;
import com.eaglebank.banking_api.dto.request.CreateUserRequest;
import com.eaglebank.banking_api.dto.response.ValidationError;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class UserValidator {
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+[1-9]\\d{1,14}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    public List<ValidationError> validate(CreateUserRequest request) {
        List<ValidationError> errors = new ArrayList<>();

        if (request.name() == null || request.name().isBlank()) {
            errors.add(new ValidationError("name", "Name is required", "MISSING_FIELD"));
        }

        if (request.email() == null || request.email().isBlank()) {
            errors.add(new ValidationError("email", "Email is required", "MISSING_FIELD"));
        } else if (!isValidEmail(request.email())) {
            errors.add(new ValidationError("email", "Email format is invalid", "INVALID_FORMAT"));
        }

        if (request.phoneNumber() == null || request.phoneNumber().isBlank()) {
            errors.add(new ValidationError("phoneNumber", "Phone number is required", "MISSING_FIELD"));
        } else if (!isValidPhoneNumber(request.phoneNumber())) {
            errors.add(new ValidationError(
                    "phoneNumber", "Phone number format is invalid (must be E.164 format)", "INVALID_FORMAT"));
        }

        if (request.address() == null) {
            errors.add(new ValidationError("address", "Address is required", "MISSING_FIELD"));
        } else {
            List<ValidationError> addressErrors = validateAddress(request.address());
            errors.addAll(addressErrors);
        }

        return errors;
    }

    private List<ValidationError> validateAddress(AddressDto address) {
        List<ValidationError> errors = new ArrayList<>();

        if (address.line1() == null || address.line1().isBlank()) {
            errors.add(new ValidationError("address.line1", "Address line 1 is required", "MISSING_FIELD"));
        }

        if (address.town() == null || address.town().isBlank()) {
            errors.add(new ValidationError("address.town", "Town is required", "MISSING_FIELD"));
        }

        if (address.county() == null || address.county().isBlank()) {
            errors.add(new ValidationError("address.county", "County is required", "MISSING_FIELD"));
        }

        if (address.postcode() == null || address.postcode().isBlank()) {
            errors.add(new ValidationError("address.postcode", "Postcode is required", "MISSING_FIELD"));
        }

        return errors;
    }

    private boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        return PHONE_PATTERN.matcher(phoneNumber).matches();
    }
}
