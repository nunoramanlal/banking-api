package com.eaglebank.banking_api.mapper.user;

import com.eaglebank.banking_api.dto.AddressDto;
import com.eaglebank.banking_api.dto.response.UserResponse;
import com.eaglebank.banking_api.entity.User;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class UserResponseMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                toAddressDto(user),
                user.getPhoneNumber(),
                user.getEmail(),
                user.getCreatedTimestamp().format(FORMATTER),
                user.getUpdatedTimestamp().format(FORMATTER));
    }

    private AddressDto toAddressDto(User user) {
        return new AddressDto(
                user.getLine1(),
                user.getLine2(),
                user.getLine3(),
                user.getTown(),
                user.getCounty(),
                user.getPostcode());
    }
}
