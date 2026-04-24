package com.eaglebank.banking_api.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eaglebank.banking_api.dto.AddressDto;
import com.eaglebank.banking_api.dto.request.CreateBankAccountRequest;
import com.eaglebank.banking_api.dto.request.CreateUserRequest;
import com.eaglebank.banking_api.dto.request.LoginRequest;
import com.eaglebank.banking_api.dto.response.ValidationErrorType;
import com.eaglebank.banking_api.repository.AccountRepository;
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
class AccountControllerIT {

    private static final String TEST_NAME = "test-name";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PHONE = "+447911123456";
    private static final String TEST_LINE1 = "test-line1";
    private static final String TEST_TOWN = "test-town";
    private static final String TEST_COUNTY = "test-county";
    private static final String TEST_POSTCODE = "TEST 123";

    private static final String ACCOUNTS_ENDPOINT = "/v1/accounts";
    private static final String ACCOUNT_NAME = "Personal Bank Account";
    private static final String ACCOUNT_TYPE_PERSONAL = "personal";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void cleanUp() {
        accountRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    class CreateAccount {

        @Test
        void shouldCreateAccountWhenAuthenticatedUserProvidesValidRequest() throws Exception {
            createUserAndGetId(TEST_EMAIL);
            String accessToken = loginAndGetAccessToken(TEST_EMAIL);

            CreateBankAccountRequest request = new CreateBankAccountRequest(ACCOUNT_NAME, ACCOUNT_TYPE_PERSONAL);

            mockMvc.perform(post(ACCOUNTS_ENDPOINT)
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.accountNumber").value(org.hamcrest.Matchers.matchesPattern("^01\\d{6}$")))
                    .andExpect(jsonPath("$.sortCode").value("10-10-10"))
                    .andExpect(jsonPath("$.name").value(ACCOUNT_NAME))
                    .andExpect(jsonPath("$.accountType").value(ACCOUNT_TYPE_PERSONAL))
                    .andExpect(jsonPath("$.balance").value(0.00))
                    .andExpect(jsonPath("$.currency").value("GBP"))
                    .andExpect(jsonPath("$.createdTimestamp").exists())
                    .andExpect(jsonPath("$.updatedTimestamp").exists());

            assertThat(accountRepository.findAll()).hasSize(1);
        }

        @Test
        void shouldCreateAccountWithSequentialAccountNumbers() throws Exception {
            createUserAndGetId(TEST_EMAIL);
            String accessToken = loginAndGetAccessToken(TEST_EMAIL);

            CreateBankAccountRequest request = new CreateBankAccountRequest(ACCOUNT_NAME, ACCOUNT_TYPE_PERSONAL);

            MvcResult firstResult = mockMvc.perform(post(ACCOUNTS_ENDPOINT)
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn();

            MvcResult secondResult = mockMvc.perform(post(ACCOUNTS_ENDPOINT)
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn();

            String firstAccountNumber = objectMapper
                    .readTree(firstResult.getResponse().getContentAsString())
                    .get("accountNumber")
                    .asText();

            String secondAccountNumber = objectMapper
                    .readTree(secondResult.getResponse().getContentAsString())
                    .get("accountNumber")
                    .asText();

            assertThat(firstAccountNumber).matches("^01\\d{6}$");
            assertThat(secondAccountNumber).matches("^01\\d{6}$");
            assertThat(Long.parseLong(secondAccountNumber))
                    .isEqualTo(Long.parseLong(firstAccountNumber) + 1);
        }

        @Test
        void shouldReturnBadRequestWhenRequestBodyIsEmpty() throws Exception {
            createUserAndGetId(TEST_EMAIL);
            String accessToken = loginAndGetAccessToken(TEST_EMAIL);

            mockMvc.perform(post(ACCOUNTS_ENDPOINT)
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Request body is missing or malformed"))
                    .andExpect(jsonPath("$.details").isArray());
        }

        @Test
        void shouldReturnBadRequestWhenRequestBodyPayloadIsEmpty() throws Exception {
            createUserAndGetId(TEST_EMAIL);
            String accessToken = loginAndGetAccessToken(TEST_EMAIL);

            mockMvc.perform(post(ACCOUNTS_ENDPOINT)
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid details supplied"))
                    .andExpect(jsonPath("$.details").isArray());
        }

        @Test
        void shouldReturnUnauthorizedWhenNoToken() throws Exception {
            CreateBankAccountRequest request = new CreateBankAccountRequest(ACCOUNT_NAME, ACCOUNT_TYPE_PERSONAL);

            mockMvc.perform(post(ACCOUNTS_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldReturnUnauthorizedWhenInvalidToken() throws Exception {
            CreateBankAccountRequest request = new CreateBankAccountRequest(ACCOUNT_NAME, ACCOUNT_TYPE_PERSONAL);

            mockMvc.perform(post(ACCOUNTS_ENDPOINT)
                            .header("Authorization", "Bearer invalid-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidRequests")
        void shouldReturnBadRequestForInvalidInputs(
                String description,
                CreateBankAccountRequest request,
                String expectedField,
                ValidationErrorType expectedType)
                throws Exception {
            createUserAndGetId(TEST_EMAIL);
            String accessToken = loginAndGetAccessToken(TEST_EMAIL);

            mockMvc.perform(post(ACCOUNTS_ENDPOINT)
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

        static Stream<Arguments> invalidRequests() {
            return Stream.of(
                    Arguments.of(
                            "missing name",
                            new CreateBankAccountRequest(null, ACCOUNT_TYPE_PERSONAL),
                            "name",
                            ValidationErrorType.MISSING),
                    Arguments.of(
                            "blank name",
                            new CreateBankAccountRequest("", ACCOUNT_TYPE_PERSONAL),
                            "name",
                            ValidationErrorType.INVALID_FORMAT),
                    Arguments.of(
                            "missing account type",
                            new CreateBankAccountRequest(ACCOUNT_NAME, null),
                            "accountType",
                            ValidationErrorType.MISSING),
                    Arguments.of(
                            "blank account type",
                            new CreateBankAccountRequest(ACCOUNT_NAME, ""),
                            "accountType",
                            ValidationErrorType.INVALID_FORMAT),
                    Arguments.of(
                            "invalid account type",
                            new CreateBankAccountRequest(ACCOUNT_NAME, "business"),
                            "accountType",
                            ValidationErrorType.INVALID_FORMAT));
        }
    }

    private String createUserAndGetId(String email) throws Exception {
        CreateUserRequest request = new CreateUserRequest(
                TEST_NAME,
                new AddressDto(TEST_LINE1, null, null, TEST_TOWN, TEST_COUNTY, TEST_POSTCODE),
                TEST_PHONE,
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