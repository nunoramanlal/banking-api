package com.eaglebank.banking_api.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eaglebank.banking_api.dto.AddressDto;
import com.eaglebank.banking_api.dto.request.CreateBankAccountRequest;
import com.eaglebank.banking_api.dto.request.CreateUserRequest;
import com.eaglebank.banking_api.dto.request.UpdateUserRequest;
import com.eaglebank.banking_api.dto.response.ValidationErrorType;
import com.eaglebank.banking_api.entity.User;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;

class UserControllerIT extends IntegrationUtils {

    @Nested
    class CreateUser {

        @Test
        void shouldCreateUserGivenAllRequestFields() throws Exception {
            CreateUserRequest request = new CreateUserRequest(
                    TEST_NAME,
                    new AddressDto(TEST_LINE1, TEST_LINE2, TEST_LINE3, TEST_TOWN, TEST_COUNTY, TEST_POSTCODE),
                    TEST_PHONE,
                    TEST_EMAIL);

            mockMvc.perform(post(USERS_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.matchesPattern("^usr-[A-Za-z0-9]+$")))
                    .andExpect(jsonPath("$.name").value(TEST_NAME))
                    .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                    .andExpect(jsonPath("$.phoneNumber").value(TEST_PHONE))
                    .andExpect(jsonPath("$.address.line1").value(TEST_LINE1))
                    .andExpect(jsonPath("$.address.line2").value(TEST_LINE2))
                    .andExpect(jsonPath("$.address.line3").value(TEST_LINE3))
                    .andExpect(jsonPath("$.address.town").value(TEST_TOWN))
                    .andExpect(jsonPath("$.address.county").value(TEST_COUNTY))
                    .andExpect(jsonPath("$.address.postcode").value(TEST_POSTCODE))
                    .andExpect(jsonPath("$.createdTimestamp").exists())
                    .andExpect(jsonPath("$.updatedTimestamp").exists());

            assertThat(userRepository.findAll()).hasSize(1);
        }

        @Test
        void shouldCreateUserGivenRequiredRequestFieldsOnly() throws Exception {
            CreateUserRequest request = new CreateUserRequest(
                    TEST_NAME,
                    new AddressDto(TEST_LINE1, null, null, TEST_TOWN, TEST_COUNTY, TEST_POSTCODE),
                    TEST_PHONE,
                    TEST_EMAIL);

            mockMvc.perform(post(USERS_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.matchesPattern("^usr-[A-Za-z0-9]+$")))
                    .andExpect(jsonPath("$.name").value(TEST_NAME))
                    .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                    .andExpect(jsonPath("$.phoneNumber").value(TEST_PHONE))
                    .andExpect(jsonPath("$.address.line1").value(TEST_LINE1))
                    .andExpect(jsonPath("$.address.line2").isEmpty())
                    .andExpect(jsonPath("$.address.line3").isEmpty())
                    .andExpect(jsonPath("$.address.town").value(TEST_TOWN))
                    .andExpect(jsonPath("$.address.county").value(TEST_COUNTY))
                    .andExpect(jsonPath("$.address.postcode").value(TEST_POSTCODE))
                    .andExpect(jsonPath("$.createdTimestamp").exists())
                    .andExpect(jsonPath("$.updatedTimestamp").exists());

            assertThat(userRepository.findAll()).hasSize(1);
        }

        @Test
        void shouldReturnBadRequestWhenRequestBodyIsEmpty() throws Exception {
            mockMvc.perform(post(USERS_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Request body is missing or malformed"))
                    .andExpect(jsonPath("$.details").isArray());
        }

        @Test
        void shouldReturnBadRequestWhenRequestBodyPayloadIsEmpty() throws Exception {
            mockMvc.perform(post(USERS_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid details supplied"))
                    .andExpect(jsonPath("$.details").isArray());
        }

        @Test
        void shouldReturnInternalServerErrorWhenCreatingUserWithDuplicateEmail() throws Exception {
            CreateUserRequest firstRequest =
                    new CreateUserRequest(TEST_NAME, validAddress(), TEST_PHONE, "duplicate@example.com");

            mockMvc.perform(post(USERS_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(firstRequest)))
                    .andExpect(status().isCreated());

            CreateUserRequest duplicateRequest =
                    new CreateUserRequest("different-name", validAddress(), TEST_PHONE, "duplicate@example.com");

            mockMvc.perform(post(USERS_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(duplicateRequest)))
                    .andExpect(status().is5xxServerError())
                    .andExpect(jsonPath("$.message").exists());

            assertThat(userRepository.findAll()).hasSize(1);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidRequests")
        void shouldReturnBadRequestForInvalidInputs(
                String description, CreateUserRequest request, String expectedField, ValidationErrorType expectedType)
                throws Exception {
            mockMvc.perform(post(USERS_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid details supplied"))
                    .andExpect(jsonPath("$.details").isArray())
                    .andExpect(jsonPath("$.details[0].field").value(expectedField))
                    .andExpect(jsonPath("$.details[0].message").exists())
                    .andExpect(jsonPath("$.details[0].type").value(expectedType.name()));
        }

        static Stream<Arguments> invalidRequests() {
            return Stream.of(
                    Arguments.of(
                            "invalid email",
                            new CreateUserRequest(TEST_NAME, validAddress(), TEST_PHONE, "invalid-email"),
                            "email",
                            ValidationErrorType.INVALID_FORMAT),
                    Arguments.of(
                            "missing email",
                            new CreateUserRequest(TEST_NAME, validAddress(), TEST_PHONE, null),
                            "email",
                            ValidationErrorType.MISSING),
                    Arguments.of(
                            "invalid phone format",
                            new CreateUserRequest(TEST_NAME, validAddress(), "07123456789", TEST_EMAIL),
                            "phoneNumber",
                            ValidationErrorType.INVALID_FORMAT),
                    Arguments.of(
                            "phone too short",
                            new CreateUserRequest(TEST_NAME, validAddress(), "+1", TEST_EMAIL),
                            "phoneNumber",
                            ValidationErrorType.INVALID_FORMAT),
                    Arguments.of(
                            "missing phone",
                            new CreateUserRequest(TEST_NAME, validAddress(), null, TEST_EMAIL),
                            "phoneNumber",
                            ValidationErrorType.MISSING),
                    Arguments.of(
                            "blank name",
                            new CreateUserRequest("", validAddress(), TEST_PHONE, TEST_EMAIL),
                            "name",
                            ValidationErrorType.INVALID_FORMAT),
                    Arguments.of(
                            "null name",
                            new CreateUserRequest(null, validAddress(), TEST_PHONE, TEST_EMAIL),
                            "name",
                            ValidationErrorType.MISSING),
                    Arguments.of(
                            "missing address",
                            new CreateUserRequest(TEST_NAME, null, TEST_PHONE, TEST_EMAIL),
                            "address",
                            ValidationErrorType.MISSING),
                    Arguments.of(
                            "invalid address - missing line1",
                            new CreateUserRequest(
                                    TEST_NAME,
                                    new AddressDto(null, null, null, TEST_TOWN, TEST_COUNTY, TEST_POSTCODE),
                                    TEST_PHONE,
                                    TEST_EMAIL),
                            "address.line1",
                            ValidationErrorType.MISSING),
                    Arguments.of(
                            "invalid address - missing town",
                            new CreateUserRequest(
                                    TEST_NAME,
                                    new AddressDto(TEST_LINE1, null, null, null, TEST_COUNTY, TEST_POSTCODE),
                                    TEST_PHONE,
                                    TEST_EMAIL),
                            "address.town",
                            ValidationErrorType.MISSING));
        }

        private static AddressDto validAddress() {
            return new AddressDto(TEST_LINE1, TEST_LINE2, TEST_LINE3, TEST_TOWN, TEST_COUNTY, TEST_POSTCODE);
        }
    }

    @Nested
    class FetchUser {

        @Test
        void shouldFetchUserWhenAuthenticatedAsSameUser() throws Exception {
            String userId = createUserAndGetId(TEST_EMAIL);
            String accessToken = loginAndGetAccessToken(TEST_EMAIL);

            mockMvc.perform(get(USERS_ENDPOINT + "/" + userId).header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(userId))
                    .andExpect(jsonPath("$.name").value(TEST_NAME))
                    .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                    .andExpect(jsonPath("$.phoneNumber").value(TEST_PHONE))
                    .andExpect(jsonPath("$.address.line1").value(TEST_LINE1))
                    .andExpect(jsonPath("$.address.line2").isEmpty())
                    .andExpect(jsonPath("$.address.line3").isEmpty())
                    .andExpect(jsonPath("$.address.town").value(TEST_TOWN))
                    .andExpect(jsonPath("$.address.county").value(TEST_COUNTY))
                    .andExpect(jsonPath("$.address.postcode").value(TEST_POSTCODE))
                    .andExpect(jsonPath("$.createdTimestamp").exists())
                    .andExpect(jsonPath("$.updatedTimestamp").exists());
        }

        @Test
        void shouldReturnBadRequestWhenUserIdFormatIsInvalid() throws Exception {
            createUserAndGetId(TEST_EMAIL);
            String accessToken = loginAndGetAccessToken(TEST_EMAIL);

            mockMvc.perform(get(USERS_ENDPOINT + "/invalid-id-format").header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid details supplied"))
                    .andExpect(jsonPath("$.details").isArray())
                    .andExpect(jsonPath("$.details[0].field").value("userId"))
                    .andExpect(jsonPath("$.details[0].message").value("User ID format is invalid"))
                    .andExpect(jsonPath("$.details[0].type").value(ValidationErrorType.INVALID_FORMAT.name()));
        }

        @Test
        void shouldReturnForbiddenWhenFetchingDifferentUser() throws Exception {
            createUserAndGetId("user1@example.com");
            String otherUserId = createUserAndGetId("user2@example.com");
            String accessToken = loginAndGetAccessToken("user1@example.com");

            mockMvc.perform(get(USERS_ENDPOINT + "/" + otherUserId).header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        void shouldReturnNotFoundWhenUserHasBeenDeleted() throws Exception {
            String userId = createUserAndGetId(TEST_EMAIL);
            String accessToken = loginAndGetAccessToken(TEST_EMAIL);

            userRepository.deleteById(userId);

            mockMvc.perform(get(USERS_ENDPOINT + "/" + userId).header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("User was not found"));
        }

        @Test
        void shouldReturnForbiddenWhenFetchingNonExistentUser() throws Exception {
            createUserAndGetId(TEST_EMAIL);
            String accessToken = loginAndGetAccessToken(TEST_EMAIL);

            mockMvc.perform(get(USERS_ENDPOINT + "/usr-nonexistent").header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        void shouldReturnUnauthorizedWhenNoToken() throws Exception {
            mockMvc.perform(get(USERS_ENDPOINT + "/usr-anything")).andExpect(status().isUnauthorized());
        }

        @Test
        void shouldReturnUnauthorizedWhenInvalidToken() throws Exception {
            mockMvc.perform(get(USERS_ENDPOINT + "/usr-anything").header("Authorization", "Bearer invalid-token"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class UpdateUser {

        @Test
        void shouldUpdateUserWhenAuthenticatedAsSameUser() throws Exception {
            String userId = createUserAndGetId(TEST_EMAIL);
            String accessToken = loginAndGetAccessToken(TEST_EMAIL);

            UpdateUserRequest request = new UpdateUserRequest(
                    "updated-name",
                    new AddressDto("updated-line1", "updated-line2", null, "updated-town", "updated-county", "UPD 456"),
                    "+447911999999",
                    "updated@example.com");

            mockMvc.perform(patch(USERS_ENDPOINT + "/" + userId)
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(userId))
                    .andExpect(jsonPath("$.name").value("updated-name"))
                    .andExpect(jsonPath("$.email").value("updated@example.com"))
                    .andExpect(jsonPath("$.phoneNumber").value("+447911999999"))
                    .andExpect(jsonPath("$.address.line1").value("updated-line1"))
                    .andExpect(jsonPath("$.address.line2").value("updated-line2"))
                    .andExpect(jsonPath("$.address.town").value("updated-town"))
                    .andExpect(jsonPath("$.address.postcode").value("UPD 456"));
        }

        @Test
        void shouldUpdateOnlyProvidedFields() throws Exception {
            String userId = createUserAndGetId(TEST_EMAIL);
            String accessToken = loginAndGetAccessToken(TEST_EMAIL);

            UpdateUserRequest partialRequest = new UpdateUserRequest("only-name-changed", null, null, null);

            mockMvc.perform(patch(USERS_ENDPOINT + "/" + userId)
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(partialRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("only-name-changed"))
                    .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                    .andExpect(jsonPath("$.phoneNumber").value(TEST_PHONE));
        }

        @Test
        void shouldRecordAuditFieldsOnUpdate() throws Exception {
            String userId = createUserAndGetId(TEST_EMAIL);
            String accessToken = loginAndGetAccessToken(TEST_EMAIL);

            UpdateUserRequest request = new UpdateUserRequest("updated-name", null, null, null);

            mockMvc.perform(patch(USERS_ENDPOINT + "/" + userId)
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            User user = userRepository.findById(userId).orElseThrow();
            assertThat(user.getCreatedBy()).isEqualTo("system");
            assertThat(user.getUpdatedBy()).isEqualTo(userId);
        }

        @Test
        void shouldReturnForbiddenWhenUpdatingDifferentUser() throws Exception {
            createUserAndGetId("user1@example.com");
            String otherUserId = createUserAndGetId("user2@example.com");
            String accessToken = loginAndGetAccessToken("user1@example.com");

            UpdateUserRequest request = new UpdateUserRequest("hacked-name", null, null, null);

            mockMvc.perform(patch(USERS_ENDPOINT + "/" + otherUserId)
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        void shouldReturnForbiddenWhenUserIdDoesNotExist() throws Exception {
            createUserAndGetId(TEST_EMAIL);
            String accessToken = loginAndGetAccessToken(TEST_EMAIL);

            UpdateUserRequest request = new UpdateUserRequest("name", null, null, null);

            mockMvc.perform(patch(USERS_ENDPOINT + "/usr-nonexistent")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        void shouldReturnUnauthorizedWhenNoToken() throws Exception {
            UpdateUserRequest request = new UpdateUserRequest("name", null, null, null);

            mockMvc.perform(patch(USERS_ENDPOINT + "/usr-anything")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldReturnBadRequestWhenUserIdFormatIsInvalid() throws Exception {
            createUserAndGetId(TEST_EMAIL);
            String accessToken = loginAndGetAccessToken(TEST_EMAIL);

            UpdateUserRequest request = new UpdateUserRequest("test-name", null, null, null);

            mockMvc.perform(patch(USERS_ENDPOINT + "/invalid-id-format")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid details supplied"))
                    .andExpect(jsonPath("$.details").isArray())
                    .andExpect(jsonPath("$.details[0].field").value("userId"))
                    .andExpect(jsonPath("$.details[0].message").value("User ID format is invalid"))
                    .andExpect(jsonPath("$.details[0].type").value(ValidationErrorType.INVALID_FORMAT.name()));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidUpdateRequests")
        void shouldReturnBadRequestForInvalidInputs(
                String description, UpdateUserRequest request, String expectedField, ValidationErrorType expectedType)
                throws Exception {
            String userId = createUserAndGetId(TEST_EMAIL);
            String accessToken = loginAndGetAccessToken(TEST_EMAIL);

            mockMvc.perform(patch(USERS_ENDPOINT + "/" + userId)
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid details supplied"))
                    .andExpect(jsonPath("$.details").isArray())
                    .andExpect(jsonPath("$.details[0].field").value(expectedField))
                    .andExpect(jsonPath("$.details[0].message").exists())
                    .andExpect(jsonPath("$.details[0].type").value(expectedType.name()));
        }

        static Stream<Arguments> invalidUpdateRequests() {
            return Stream.of(
                    Arguments.of(
                            "invalid email format",
                            new UpdateUserRequest(null, null, null, "invalid-email"),
                            "email",
                            ValidationErrorType.INVALID_FORMAT),
                    Arguments.of(
                            "invalid phone format",
                            new UpdateUserRequest(null, null, "07123456789", null),
                            "phoneNumber",
                            ValidationErrorType.INVALID_FORMAT),
                    Arguments.of(
                            "phone too short",
                            new UpdateUserRequest(null, null, "+1", null),
                            "phoneNumber",
                            ValidationErrorType.INVALID_FORMAT),
                    Arguments.of(
                            "invalid address - missing line1",
                            new UpdateUserRequest(
                                    null,
                                    new AddressDto(null, null, null, TEST_TOWN, TEST_COUNTY, TEST_POSTCODE),
                                    null,
                                    null),
                            "address.line1",
                            ValidationErrorType.MISSING),
                    Arguments.of(
                            "invalid address - missing town",
                            new UpdateUserRequest(
                                    null,
                                    new AddressDto(TEST_LINE1, null, null, null, TEST_COUNTY, TEST_POSTCODE),
                                    null,
                                    null),
                            "address.town",
                            ValidationErrorType.MISSING));
        }
    }

    @Nested
    class DeleteUser {

        @Test
        void shouldDeleteUserWhenAuthenticatedAndNoBankAccount() throws Exception {
            String userId = createUserAndGetId(TEST_EMAIL);
            String accessToken = loginAndGetAccessToken(TEST_EMAIL);

            mockMvc.perform(delete(USERS_ENDPOINT + "/" + userId).header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isNoContent());

            assertThat(userRepository.findById(userId)).isEmpty();
        }

        @Test
        void shouldReturnConflictWhenUserHasBankAccount() throws Exception {
            String userId = createUserAndGetId(TEST_EMAIL);
            String accessToken = loginAndGetAccessToken(TEST_EMAIL);

            // Create a bank account for this user
            CreateBankAccountRequest accountRequest = new CreateBankAccountRequest(ACCOUNT_NAME, ACCOUNT_TYPE_PERSONAL);
            mockMvc.perform(post(ACCOUNTS_ENDPOINT)
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(accountRequest)))
                    .andExpect(status().isCreated());

            mockMvc.perform(delete(USERS_ENDPOINT + "/" + userId).header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message")
                            .value("A user cannot be deleted when they are associated with a bank account"));

            // Verify user was NOT deleted
            assertThat(userRepository.findById(userId)).isPresent();
        }

        @Test
        void shouldReturnForbiddenWhenDeletingDifferentUser() throws Exception {
            createUserAndGetId("user1@example.com");
            String otherUserId = createUserAndGetId("user2@example.com");
            String accessToken = loginAndGetAccessToken("user1@example.com");

            mockMvc.perform(delete(USERS_ENDPOINT + "/" + otherUserId).header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").exists());

            // Verify the other user was NOT deleted
            assertThat(userRepository.findById(otherUserId)).isPresent();
        }

        @Test
        void shouldReturnForbiddenWhenDeletingNonExistentUser() throws Exception {
            createUserAndGetId(TEST_EMAIL);
            String accessToken = loginAndGetAccessToken(TEST_EMAIL);

            mockMvc.perform(delete(USERS_ENDPOINT + "/usr-nonexistent")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        void shouldReturnUnauthorizedWhenNoToken() throws Exception {
            mockMvc.perform(delete(USERS_ENDPOINT + "/usr-anything")).andExpect(status().isUnauthorized());
        }

        @Test
        void shouldReturnUnauthorizedWhenInvalidToken() throws Exception {
            mockMvc.perform(delete(USERS_ENDPOINT + "/usr-anything").header("Authorization", "Bearer invalid-token"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldReturnBadRequestWhenUserIdFormatIsInvalid() throws Exception {
            createUserAndGetId(TEST_EMAIL);
            String accessToken = loginAndGetAccessToken(TEST_EMAIL);

            mockMvc.perform(delete(USERS_ENDPOINT + "/invalid-id-format")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid details supplied"))
                    .andExpect(jsonPath("$.details").isArray())
                    .andExpect(jsonPath("$.details[0].field").value("userId"))
                    .andExpect(jsonPath("$.details[0].message").value("User ID format is invalid"))
                    .andExpect(jsonPath("$.details[0].type").value(ValidationErrorType.INVALID_FORMAT.name()));
        }
    }
}
