package com.eaglebank.banking_api.mapper.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.eaglebank.banking_api.entity.User;
import com.eaglebank.banking_api.service.command.CreateUserCommand;
import com.eaglebank.banking_api.service.command.UpdateUserCommand;
import org.junit.jupiter.api.Test;

class UserEntityMapperTest {

    private final UserEntityMapper mapper = new UserEntityMapper();

    @Test
    void shouldMapAllFieldsFromCommandToEntity() {
        CreateUserCommand command = new CreateUserCommand(
                "test-name",
                "test-line1",
                "test-line2",
                "test-line3",
                "test-town",
                "test-county",
                "TEST 123",
                "+447911123456",
                "test@example.com");

        User user = mapper.toEntity(command);

        assertThat(user.getName()).isEqualTo("test-name");
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getPhoneNumber()).isEqualTo("+447911123456");
        assertThat(user.getLine1()).isEqualTo("test-line1");
        assertThat(user.getLine2()).isEqualTo("test-line2");
        assertThat(user.getLine3()).isEqualTo("test-line3");
        assertThat(user.getTown()).isEqualTo("test-town");
        assertThat(user.getCounty()).isEqualTo("test-county");
        assertThat(user.getPostcode()).isEqualTo("TEST 123");
    }

    @Test
    void shouldMapNullOptionalAddressFields() {
        CreateUserCommand command = new CreateUserCommand(
                "test-name",
                "test-line1",
                null,
                null,
                "test-town",
                "test-county",
                "TEST 123",
                "+447911123456",
                "test@example.com");

        User user = mapper.toEntity(command);

        assertThat(user.getLine2()).isNull();
        assertThat(user.getLine3()).isNull();
        assertThat(user.getLine1()).isEqualTo("test-line1");
    }

    @Test
    void shouldNotSetIdOrTimestamps() {
        CreateUserCommand command = new CreateUserCommand(
                "test-name",
                "test-line1",
                null,
                null,
                "test-town",
                "test-county",
                "TEST 123",
                "+447911123456",
                "test@example.com");

        User user = mapper.toEntity(command);

        assertThat(user.getId()).isNull();
        assertThat(user.getCreatedTimestamp()).isNull();
        assertThat(user.getUpdatedTimestamp()).isNull();
    }

    @Test
    void shouldApplyAllPatchFieldsToEntity() {
        User user = buildUser();
        UpdateUserCommand command = new UpdateUserCommand(
                "updated-name",
                "updated-line1",
                "updated-line2",
                "updated-line3",
                "updated-town",
                "updated-county",
                "UPD 456",
                "+447911999999",
                "updated@example.com");

        mapper.applyPatch(command, user);

        assertThat(user.getName()).isEqualTo("updated-name");
        assertThat(user.getEmail()).isEqualTo("updated@example.com");
        assertThat(user.getPhoneNumber()).isEqualTo("+447911999999");
        assertThat(user.getLine1()).isEqualTo("updated-line1");
        assertThat(user.getLine2()).isEqualTo("updated-line2");
        assertThat(user.getLine3()).isEqualTo("updated-line3");
        assertThat(user.getTown()).isEqualTo("updated-town");
        assertThat(user.getCounty()).isEqualTo("updated-county");
        assertThat(user.getPostcode()).isEqualTo("UPD 456");
    }

    @Test
    void shouldNotOverwriteFieldsWhenPatchValueIsNull() {
        User user = buildUser();
        UpdateUserCommand command = new UpdateUserCommand(null, null, null, null, null, null, null, null, null);

        mapper.applyPatch(command, user);

        assertThat(user.getName()).isEqualTo("test-name");
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getPhoneNumber()).isEqualTo("+447911123456");
        assertThat(user.getLine1()).isEqualTo("test-line1");
        assertThat(user.getLine2()).isEqualTo("test-line2");
        assertThat(user.getLine3()).isEqualTo("test-line3");
        assertThat(user.getTown()).isEqualTo("test-town");
        assertThat(user.getCounty()).isEqualTo("test-county");
        assertThat(user.getPostcode()).isEqualTo("TEST 123");
    }

    @Test
    void shouldApplyOnlyNonNullPatchFields() {
        User user = buildUser();
        UpdateUserCommand command =
                new UpdateUserCommand("updated-name", null, null, null, null, null, null, null, null);

        mapper.applyPatch(command, user);

        assertThat(user.getName()).isEqualTo("updated-name");
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getPhoneNumber()).isEqualTo("+447911123456");
        assertThat(user.getLine1()).isEqualTo("test-line1");
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
        return user;
    }
}
