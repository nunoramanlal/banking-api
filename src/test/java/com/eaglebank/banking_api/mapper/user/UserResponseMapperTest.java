package com.eaglebank.banking_api.mapper.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.eaglebank.banking_api.dto.response.UserResponse;
import com.eaglebank.banking_api.entity.User;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class UserResponseMapperTest {

    private static final LocalDateTime FIXED_CREATED = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
    private static final LocalDateTime FIXED_UPDATED = LocalDateTime.of(2024, 2, 20, 14, 45, 30);

    private final UserResponseMapper mapper = new UserResponseMapper();

    @Test
    void shouldMapAllFieldsFromEntityToResponse() {
        User user = buildUser();

        UserResponse response = mapper.toResponse(user);

        assertThat(response.id()).isEqualTo("usr-test123");
        assertThat(response.name()).isEqualTo("test-name");
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.phoneNumber()).isEqualTo("+447911123456");
        assertThat(response.address().line1()).isEqualTo("test-line1");
        assertThat(response.address().line2()).isEqualTo("test-line2");
        assertThat(response.address().line3()).isEqualTo("test-line3");
        assertThat(response.address().town()).isEqualTo("test-town");
        assertThat(response.address().county()).isEqualTo("test-county");
        assertThat(response.address().postcode()).isEqualTo("TEST 123");
    }

    @Test
    void shouldMapNullOptionalAddressFields() {
        User user = buildUser();
        user.setLine2(null);
        user.setLine3(null);

        UserResponse response = mapper.toResponse(user);

        assertThat(response.address().line2()).isNull();
        assertThat(response.address().line3()).isNull();
        assertThat(response.address().line1()).isEqualTo("test-line1");
    }

    @Test
    void shouldFormatTimestampsInIsoFormat() {
        User user = buildUser();

        UserResponse response = mapper.toResponse(user);

        assertThat(response.createdTimestamp()).isEqualTo("2024-01-15T10:30:00");
        assertThat(response.updatedTimestamp()).isEqualTo("2024-02-20T14:45:30");
    }

    private User buildUser() {
        User user = new User(
                "test-name",
                "test@example.com",
                "+447911123456",
                "test-line1",
                "test-line2",
                "test-line3",
                "test-town",
                "test-county",
                "TEST 123");
        user.setId("usr-test123");
        user.setCreatedTimestamp(FIXED_CREATED);
        user.setUpdatedTimestamp(FIXED_UPDATED);
        return user;
    }
}
