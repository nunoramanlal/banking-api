package com.eaglebank.banking_api.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eaglebank.banking_api.dto.AddressDto;
import com.eaglebank.banking_api.dto.request.CreateUserRequest;
import com.eaglebank.banking_api.dto.request.LoginRequest;
import com.eaglebank.banking_api.dto.response.ValidationErrorType;
import com.eaglebank.banking_api.repository.RefreshTokenRepository;
import com.eaglebank.banking_api.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void cleanUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    class UserCreation {

        @Test
        void shouldCreateUserGivenAllRequestFields() throws Exception {
            CreateUserRequest request = new CreateUserRequest(
                    "test-name",
                    new AddressDto("test-line1", "test-line2", "test-line3", "test-town", "test-county", "TEST 123"),
                    "+447911123456",
                    "test@example.com");

            mockMvc.perform(post("/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.matchesPattern("^usr-[A-Za-z0-9]+$")))
                    .andExpect(jsonPath("$.name").value("test-name"))
                    .andExpect(jsonPath("$.email").value("test@example.com"))
                    .andExpect(jsonPath("$.phoneNumber").value("+447911123456"))
                    .andExpect(jsonPath("$.address.line1").value("test-line1"))
                    .andExpect(jsonPath("$.address.line2").value("test-line2"))
                    .andExpect(jsonPath("$.address.line3").value("test-line3"))
                    .andExpect(jsonPath("$.address.town").value("test-town"))
                    .andExpect(jsonPath("$.address.county").value("test-county"))
                    .andExpect(jsonPath("$.address.postcode").value("TEST 123"))
                    .andExpect(jsonPath("$.createdTimestamp").exists())
                    .andExpect(jsonPath("$.updatedTimestamp").exists());

            assertThat(userRepository.findAll()).hasSize(1);
        }

        @Test
        void shouldCreateUserGivenRequiredRequestFieldsOnly() throws Exception {
            CreateUserRequest request = new CreateUserRequest(
                    "test-name",
                    new AddressDto("test-line1", null, null, "test-town", "test-county", "TEST 123"),
                    "+447700900123",
                    "test@example.com");

            mockMvc.perform(post("/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.matchesPattern("^usr-[A-Za-z0-9]+$")))
                    .andExpect(jsonPath("$.name").value("test-name"))
                    .andExpect(jsonPath("$.email").value("test@example.com"))
                    .andExpect(jsonPath("$.phoneNumber").value("+447700900123"))
                    .andExpect(jsonPath("$.address.line1").value("test-line1"))
                    .andExpect(jsonPath("$.address.line2").isEmpty())
                    .andExpect(jsonPath("$.address.line3").isEmpty())
                    .andExpect(jsonPath("$.address.town").value("test-town"))
                    .andExpect(jsonPath("$.address.county").value("test-county"))
                    .andExpect(jsonPath("$.address.postcode").value("TEST 123"))
                    .andExpect(jsonPath("$.createdTimestamp").exists())
                    .andExpect(jsonPath("$.updatedTimestamp").exists());

            assertThat(userRepository.findAll()).hasSize(1);
        }

        @Test
        void shouldReturnBadRequestWhenRequestBodyIsEmpty() throws Exception {
            mockMvc.perform(post("/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Request body is missing or malformed"))
                    .andExpect(jsonPath("$.details").isArray());
        }

        @Test
        void shouldReturnBadRequestWhenRequestBodyPayloadIsEmpty() throws Exception {
            mockMvc.perform(post("/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid details supplied"))
                    .andExpect(jsonPath("$.details").isArray());
        }

        @Test
        void shouldReturnInternalServerErrorWhenCreatingUserWithDuplicateEmail() throws Exception {
            CreateUserRequest firstRequest =
                    new CreateUserRequest("test-name", validAddress(), "+447911123456", "duplicate@example.com");

            mockMvc.perform(post("/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(firstRequest)))
                    .andExpect(status().isCreated());

            CreateUserRequest duplicateRequest =
                    new CreateUserRequest("different-name", validAddress(), "+447911999999", "duplicate@example.com");

            mockMvc.perform(post("/v1/users")
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
            mockMvc.perform(post("/v1/users")
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
                            new CreateUserRequest("test-name", validAddress(), "+447911123456", "invalid-email"),
                            "email",
                            ValidationErrorType.INVALID_FORMAT),
                    Arguments.of(
                            "missing email",
                            new CreateUserRequest("test-name", validAddress(), "+447911123456", null),
                            "email",
                            ValidationErrorType.MISSING),
                    Arguments.of(
                            "invalid phone format",
                            new CreateUserRequest("test-name", validAddress(), "07123456789", "test@example.com"),
                            "phoneNumber",
                            ValidationErrorType.INVALID_FORMAT),
                    Arguments.of(
                            "phone too short",
                            new CreateUserRequest("test-name", validAddress(), "+1", "test@example.com"),
                            "phoneNumber",
                            ValidationErrorType.INVALID_FORMAT),
                    Arguments.of(
                            "missing phone",
                            new CreateUserRequest("test-name", validAddress(), null, "test@example.com"),
                            "phoneNumber",
                            ValidationErrorType.MISSING),
                    Arguments.of(
                            "blank name",
                            new CreateUserRequest("", validAddress(), "+447911123456", "test@example.com"),
                            "name",
                            ValidationErrorType.INVALID_FORMAT),
                    Arguments.of(
                            "null name",
                            new CreateUserRequest(null, validAddress(), "+447911123456", "test@example.com"),
                            "name",
                            ValidationErrorType.MISSING),
                    Arguments.of(
                            "missing address",
                            new CreateUserRequest("test-name", null, "+447911123456", "test@example.com"),
                            "address",
                            ValidationErrorType.MISSING),
                    Arguments.of(
                            "invalid address - missing line1",
                            new CreateUserRequest(
                                    "test-name",
                                    new AddressDto(null, null, null, "test-town", "test-county", "TEST 123"),
                                    "+447911123456",
                                    "test@example.com"),
                            "address.line1",
                            ValidationErrorType.MISSING),
                    Arguments.of(
                            "invalid address - missing town",
                            new CreateUserRequest(
                                    "test-name",
                                    new AddressDto("test-line1", null, null, null, "test-county", "TEST 123"),
                                    "+447911123456",
                                    "test@example.com"),
                            "address.town",
                            ValidationErrorType.MISSING));
        }

        private static AddressDto validAddress() {
            return new AddressDto("test-line1", "test-line2", "test-line3", "test-town", "test-county", "TEST 123");
        }
    }

    @Nested
    class FetchUser {

        @Test
        void shouldFetchUserWhenAuthenticatedAsSameUser() throws Exception {
            String userId = createUserAndGetId("test@example.com");
            String accessToken = loginAndGetAccessToken("test@example.com");

            mockMvc.perform(get("/v1/users/" + userId).header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(userId))
                    .andExpect(jsonPath("$.name").value("test-name"))
                    .andExpect(jsonPath("$.email").value("test@example.com"))
                    .andExpect(jsonPath("$.phoneNumber").value("+447911123456"))
                    .andExpect(jsonPath("$.address.line1").value("test-line1"))
                    .andExpect(jsonPath("$.address.line2").isEmpty())
                    .andExpect(jsonPath("$.address.line3").isEmpty())
                    .andExpect(jsonPath("$.address.town").value("test-town"))
                    .andExpect(jsonPath("$.address.county").value("test-county"))
                    .andExpect(jsonPath("$.address.postcode").value("TEST 123"))
                    .andExpect(jsonPath("$.createdTimestamp").exists())
                    .andExpect(jsonPath("$.updatedTimestamp").exists());
        }

        @Test
        void shouldReturnBadRequestWhenUserIdFormatIsInvalid() throws Exception {
            createUserAndGetId("test@example.com");
            String accessToken = loginAndGetAccessToken("test@example.com");

            mockMvc.perform(get("/v1/users/invalid-id-format").header("Authorization", "Bearer " + accessToken))
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

            mockMvc.perform(get("/v1/users/" + otherUserId).header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        void shouldReturnForbiddenWhenFetchingNonExistentUser() throws Exception {
            createUserAndGetId("test@example.com");
            String accessToken = loginAndGetAccessToken("test@example.com");

            mockMvc.perform(get("/v1/users/usr-nonexistent").header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        void shouldReturnUnauthorizedWhenNoToken() throws Exception {
            mockMvc.perform(get("/v1/users/usr-anything")).andExpect(status().isUnauthorized());
        }

        @Test
        void shouldReturnUnauthorizedWhenInvalidToken() throws Exception {
            mockMvc.perform(get("/v1/users/usr-anything").header("Authorization", "Bearer invalid-token"))
                    .andExpect(status().isUnauthorized());
        }
    }

    private String createUserAndGetId(String email) throws Exception {
        CreateUserRequest request = new CreateUserRequest(
                "test-name",
                new AddressDto("test-line1", null, null, "test-town", "test-county", "TEST 123"),
                "+447911123456",
                email);

        MvcResult result = mockMvc.perform(post("/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("id")
                .asText();
    }

    private String loginAndGetAccessToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email))))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("accessToken")
                .asText();
    }
}