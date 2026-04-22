package com.eaglebank.banking_api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.eaglebank.banking_api.dto.AddressDto;
import com.eaglebank.banking_api.dto.request.CreateUserRequest;
import com.eaglebank.banking_api.dto.response.UserResponse;
import com.eaglebank.banking_api.entity.Address;
import com.eaglebank.banking_api.entity.User;
import com.eaglebank.banking_api.service.command.CreateUserCommand;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserMapperTest {

    private final UserMapper mapper = new UserMapper();

    @Test
    void shouldMapCreateUserRequestToCommand() {

        CreateUserRequest request = new CreateUserRequest(
                "John Doe",
                new AddressDto(
                        "10 Downing Street", "Westminster", "London Office", "London", "Greater London", "SW1A 2AA"),
                "+447911123456",
                "john.doe@example.com");

        CreateUserCommand command = mapper.toCommand(request);

        assertThat(command.name()).isEqualTo("John Doe");
        assertThat(command.email()).isEqualTo("john.doe@example.com");
        assertThat(command.phoneNumber()).isEqualTo("+447911123456");

        assertThat(command.address()).isNotNull();
        assertThat(command.address().line1()).isEqualTo("10 Downing Street");
        assertThat(command.address().line2()).isEqualTo("Westminster");
        assertThat(command.address().line3()).isEqualTo("London Office");
        assertThat(command.address().town()).isEqualTo("London");
        assertThat(command.address().county()).isEqualTo("Greater London");
        assertThat(command.address().postcode()).isEqualTo("SW1A 2AA");
    }

    @Test
    void shouldMapUserToUserResponse() {

        User user = buildUser(
                "John Doe",
                "john.doe@example.com",
                "+447911123456",
                "10 Downing Street",
                "Westminster",
                "London Office",
                "London",
                "Greater London",
                "SW1A 2AA");

        UserResponse response = mapper.toResponse(user);

        assertThat(response.id()).isEqualTo(user.getId().toString());
        assertThat(response.name()).isEqualTo("John Doe");
        assertThat(response.email()).isEqualTo("john.doe@example.com");
        assertThat(response.phoneNumber()).isEqualTo("+447911123456");

        assertThat(response.address().line1()).isEqualTo("10 Downing Street");
        assertThat(response.address().line2()).isEqualTo("Westminster");
        assertThat(response.address().line3()).isEqualTo("London Office");
        assertThat(response.address().town()).isEqualTo("London");
        assertThat(response.address().county()).isEqualTo("Greater London");
        assertThat(response.address().postcode()).isEqualTo("SW1A 2AA");

        assertThat(response.createdTimestamp()).isNotBlank();
        assertThat(response.updatedTimestamp()).isNotBlank();
    }

    @Test
    void shouldMapUserResponseWithNullOptionalAddressFields() {

        User user = buildUser(
                "Alice",
                "alice@example.com",
                "+447700900123",
                "221B Baker Street",
                null,
                null,
                "London",
                "Greater London",
                "NW1 6XE");

        UserResponse response = mapper.toResponse(user);

        assertThat(response.address().line1()).isEqualTo("221B Baker Street");
        assertThat(response.address().line2()).isNull();
        assertThat(response.address().line3()).isNull();
        assertThat(response.address().town()).isEqualTo("London");
        assertThat(response.address().county()).isEqualTo("Greater London");
        assertThat(response.address().postcode()).isEqualTo("NW1 6XE");
    }

    @Test
    void shouldMapUserResponseWithFormattedTimestamps() {

        LocalDateTime created = LocalDateTime.of(2025, 1, 10, 10, 30, 0);
        LocalDateTime updated = LocalDateTime.of(2025, 1, 10, 12, 45, 0);

        User user = buildUser(
                "John Doe",
                "john.doe@example.com",
                "+447911123456",
                "10 Downing Street",
                "Westminster",
                "London Office",
                "London",
                "Greater London",
                "SW1A 2AA");

        user.setCreatedTimestamp(created);
        user.setUpdatedTimestamp(updated);

        UserResponse response = mapper.toResponse(user);

        assertThat(response.createdTimestamp()).isEqualTo("2025-01-10T10:30:00");

        assertThat(response.updatedTimestamp()).isEqualTo("2025-01-10T12:45:00");
    }

    // -------- helper --------

    private User buildUser(
            String name,
            String email,
            String phone,
            String line1,
            String line2,
            String line3,
            String town,
            String county,
            String postcode) {
        User user = new User();

        user.setId(UUID.randomUUID());
        user.setName(name);
        user.setEmail(email);
        user.setPhoneNumber(phone);

        Address address = new Address();
        address.setLine1(line1);
        address.setLine2(line2);
        address.setLine3(line3);
        address.setTown(town);
        address.setCounty(county);
        address.setPostcode(postcode);

        user.setAddress(address);

        user.setCreatedTimestamp(LocalDateTime.now());
        user.setUpdatedTimestamp(LocalDateTime.now());

        return user;
    }
}
