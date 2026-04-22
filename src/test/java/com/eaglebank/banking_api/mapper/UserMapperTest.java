package com.eaglebank.banking_api.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import com.eaglebank.banking_api.dto.AddressDto;
import com.eaglebank.banking_api.dto.request.CreateUserRequest;
import com.eaglebank.banking_api.dto.response.UserResponse;
import com.eaglebank.banking_api.entity.User;
import com.eaglebank.banking_api.service.command.CreateUserCommand;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserMapperTest {

    private final UserMapper mapper = new UserMapper();

    private static final LocalDateTime FIXED_CREATED =
            LocalDateTime.of(2025, 1, 1, 10, 0, 0);

    private static final LocalDateTime FIXED_UPDATED =
            LocalDateTime.of(2025, 1, 2, 12, 30, 0);

    @Test
    void shouldMapCreateUserRequestToCommand() {
        CreateUserRequest request = new CreateUserRequest(
                "test-name",
                new AddressDto(
                        "test-line1",
                        "test-line2",
                        "test-line3",
                        "test-town",
                        "test-county",
                        "TEST123"
                ),
                "+10000000000",
                "test@example.com"
        );

        CreateUserCommand command = mapper.toCommand(request);

        assertThat(command.name()).isEqualTo("test-name");
        assertThat(command.email()).isEqualTo("test@example.com");
        assertThat(command.phoneNumber()).isEqualTo("+10000000000");
        assertThat(command.line1()).isEqualTo("test-line1");
        assertThat(command.line2()).isEqualTo("test-line2");
        assertThat(command.line3()).isEqualTo("test-line3");
        assertThat(command.town()).isEqualTo("test-town");
        assertThat(command.county()).isEqualTo("test-county");
        assertThat(command.postcode()).isEqualTo("TEST123");
    }

    @Test
    void shouldMapUserToUserResponse() {
        User user = buildUser();

        UserResponse response = mapper.toResponse(user);

        assertThat(response.id()).isEqualTo(user.getId().toString());
        assertThat(response.name()).isEqualTo("test-name");
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.phoneNumber()).isEqualTo("+10000000000");
        assertThat(response.address().line1()).isEqualTo("test-line1");
        assertThat(response.address().line2()).isEqualTo("test-line2");
        assertThat(response.address().line3()).isEqualTo("test-line3");
        assertThat(response.address().town()).isEqualTo("test-town");
        assertThat(response.address().county()).isEqualTo("test-county");
        assertThat(response.address().postcode()).isEqualTo("TEST123");
        assertThat(response.createdTimestamp()).isEqualTo("2025-01-01T10:00:00");
        assertThat(response.updatedTimestamp()).isEqualTo("2025-01-02T12:30:00");
    }

    @Test
    void shouldMapUserResponseWithNullOptionalAddressFields() {
        User user = buildUser();
        user.setLine2(null);
        user.setLine3(null);

        UserResponse response = mapper.toResponse(user);

        assertThat(response.address().line2()).isNull();
        assertThat(response.address().line3()).isNull();
    }

    @Test
    void shouldMapUserResponseWithFormattedTimestamps() {
        User user = buildUser();
        user.setCreatedTimestamp(FIXED_CREATED);
        user.setUpdatedTimestamp(FIXED_UPDATED);

        UserResponse response = mapper.toResponse(user);

        assertThat(response.createdTimestamp()).isEqualTo("2025-01-01T10:00:00");
        assertThat(response.updatedTimestamp()).isEqualTo("2025-01-02T12:30:00");
    }

    // -------- helper --------

    private User buildUser() {
        User user = new User();

        user.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        user.setName("test-name");
        user.setEmail("test@example.com");
        user.setPhoneNumber("+10000000000");
        user.setLine1("test-line1");
        user.setLine2("test-line2");
        user.setLine3("test-line3");
        user.setTown("test-town");
        user.setCounty("test-county");
        user.setPostcode("TEST123");
        user.setCreatedTimestamp(FIXED_CREATED);
        user.setUpdatedTimestamp(FIXED_UPDATED);

        return user;
    }
}