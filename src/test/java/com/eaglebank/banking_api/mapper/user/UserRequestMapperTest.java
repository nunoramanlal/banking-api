package com.eaglebank.banking_api.mapper.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.eaglebank.banking_api.dto.AddressDto;
import com.eaglebank.banking_api.dto.request.CreateUserRequest;
import com.eaglebank.banking_api.service.command.CreateUserCommand;
import org.junit.jupiter.api.Test;

class UserRequestMapperTest {

    private final UserRequestMapper mapper = new UserRequestMapper();

    @Test
    void shouldMapAllFieldsFromRequestToCommand() {
        CreateUserRequest request = new CreateUserRequest(
                "test-name",
                new AddressDto("test-line1", "test-line2", "test-line3", "test-town", "test-county", "TEST 123"),
                "+447911123456",
                "test@example.com");

        CreateUserCommand command = mapper.toCommand(request);

        assertThat(command.name()).isEqualTo("test-name");
        assertThat(command.email()).isEqualTo("test@example.com");
        assertThat(command.phoneNumber()).isEqualTo("+447911123456");
        assertThat(command.line1()).isEqualTo("test-line1");
        assertThat(command.line2()).isEqualTo("test-line2");
        assertThat(command.line3()).isEqualTo("test-line3");
        assertThat(command.town()).isEqualTo("test-town");
        assertThat(command.county()).isEqualTo("test-county");
        assertThat(command.postcode()).isEqualTo("TEST 123");
    }

    @Test
    void shouldMapNullOptionalAddressFields() {
        CreateUserRequest request = new CreateUserRequest(
                "test-name",
                new AddressDto("test-line1", null, null, "test-town", "test-county", "TEST 123"),
                "+447911123456",
                "test@example.com");

        CreateUserCommand command = mapper.toCommand(request);

        assertThat(command.line2()).isNull();
        assertThat(command.line3()).isNull();
        assertThat(command.line1()).isEqualTo("test-line1");
    }
}
