package com.eaglebank.banking_api.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eaglebank.banking_api.dto.AddressDto;
import com.eaglebank.banking_api.dto.request.CreateUserRequest;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Nested
    class UserCreation {

        @Test
        void shouldCreateUserGivenAllRequestFields() throws Exception {

            CreateUserRequest request =
                    new CreateUserRequest("John Doe", validAddress(), "+447911123456", "john.doe@example.com");

            mockMvc.perform(post("/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.name").value("John Doe"))
                    .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                    .andExpect(jsonPath("$.phoneNumber").value("+447911123456"))
                    .andExpect(jsonPath("$.address.line1").value("10 Downing Street"))
                    .andExpect(jsonPath("$.address.line2").value("Westminster"))
                    .andExpect(jsonPath("$.address.line3").value("England"))
                    .andExpect(jsonPath("$.address.town").value("London"))
                    .andExpect(jsonPath("$.address.county").value("Greater London"))
                    .andExpect(jsonPath("$.address.postcode").value("123 456"))
                    .andExpect(jsonPath("$.createdTimestamp").exists())
                    .andExpect(jsonPath("$.updatedTimestamp").exists());

            assertThat(userRepository.findAll()).hasSize(1);
        }

        @Test
        void shouldCreateUserGivenRequiredRequestFieldsOnly() throws Exception {

            CreateUserRequest request = new CreateUserRequest(
                    "Alice Smith",
                    new AddressDto("221B Baker Street", null, null, "London", "Greater London", "NW1 6XE"),
                    "+447700900123",
                    "alice.smith@example.com");

            mockMvc.perform(post("/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())

                    // Root fields
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.name").value("Alice Smith"))
                    .andExpect(jsonPath("$.email").value("alice.smith@example.com"))
                    .andExpect(jsonPath("$.phoneNumber").value("+447700900123"))
                    .andExpect(jsonPath("$.address.line1").value("221B Baker Street"))
                    .andExpect(jsonPath("$.address.line2").isEmpty())
                    .andExpect(jsonPath("$.address.line3").isEmpty())
                    .andExpect(jsonPath("$.address.town").value("London"))
                    .andExpect(jsonPath("$.address.county").value("Greater London"))
                    .andExpect(jsonPath("$.address.postcode").value("NW1 6XE"))
                    .andExpect(jsonPath("$.createdTimestamp").exists())
                    .andExpect(jsonPath("$.updatedTimestamp").exists());

            assertThat(userRepository.findAll()).hasSize(1);
        }

        @Test
        void shouldReturnBadRequestWhenRequestBodyIsEmpty() throws Exception {
            mockMvc.perform(post("/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(""))
                    .andExpect(status().isBadRequest());
        }

        // ---------- PARAMETERIZED BAD REQUEST TESTS ----------

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidRequests")
        void shouldReturnBadRequestForInvalidInputs(String description, CreateUserRequest request, String expectedField)
                throws Exception {
            mockMvc.perform(post("/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.details").isArray())
                    .andExpect(jsonPath("$.details[0].field").value(expectedField))
                    .andExpect(jsonPath("$.details[0].message").exists());
        }

        static Stream<Arguments> invalidRequests() {
            return Stream.of(
                    Arguments.of(
                            "invalid email",
                            new CreateUserRequest("John", validAddress(), "+447911123456", "invalid"),
                            "email"),
                    Arguments.of(
                            "missing email",
                            new CreateUserRequest("John", validAddress(), "+447911123456", null),
                            "email"),
                    Arguments.of(
                            "invalid phone format",
                            new CreateUserRequest("John", validAddress(), "07123456789", "test@test.com"),
                            "phoneNumber"),
                    Arguments.of(
                            "phone too short",
                            new CreateUserRequest("John", validAddress(), "+1", "test@test.com"),
                            "phoneNumber"),
                    Arguments.of(
                            "missing phone",
                            new CreateUserRequest("John", validAddress(), null, "test@test.com"),
                            "phoneNumber"),
                    Arguments.of(
                            "blank name",
                            new CreateUserRequest("", validAddress(), "+447911123456", "test@test.com"),
                            "name"),
                    Arguments.of(
                            "null name",
                            new CreateUserRequest(null, validAddress(), "+447911123456", "test@test.com"),
                            "name"),
                    Arguments.of(
                            "missing address",
                            new CreateUserRequest("John", null, "+447911123456", "test@test.com"),
                            "address"),
                    Arguments.of(
                            "invalid address - missing line1",
                            new CreateUserRequest(
                                    "John",
                                    new AddressDto(null, null, null, "London", "County", "SW1A"),
                                    "+447911123456",
                                    "test@test.com"),
                            "address.line1"),
                    Arguments.of(
                            "invalid address - missing town",
                            new CreateUserRequest(
                                    "John",
                                    new AddressDto("Line1", null, null, null, "County", "SW1A"),
                                    "+447911123456",
                                    "test@test.com"),
                            "address.town"));
        }

        private static AddressDto validAddress() {
            return new AddressDto("10 Downing Street", "Westminster", "England", "London", "Greater London", "123 456");
        }
    }
}
