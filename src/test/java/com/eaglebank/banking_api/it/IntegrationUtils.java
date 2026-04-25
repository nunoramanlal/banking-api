package com.eaglebank.banking_api.it;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eaglebank.banking_api.dto.AddressDto;
import com.eaglebank.banking_api.dto.request.CreateBankAccountRequest;
import com.eaglebank.banking_api.dto.request.CreateUserRequest;
import com.eaglebank.banking_api.dto.request.LoginRequest;
import com.eaglebank.banking_api.repository.AccountRepository;
import com.eaglebank.banking_api.repository.RefreshTokenRepository;
import com.eaglebank.banking_api.repository.TransactionRepository;
import com.eaglebank.banking_api.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Base class for integration tests.
 *
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class IntegrationUtils {

    protected static final String TEST_NAME = "test-name";
    protected static final String TEST_EMAIL = "test@example.com";
    protected static final String TEST_PHONE = "+447911123456";
    protected static final String TEST_LINE1 = "test-line1";
    protected static final String TEST_LINE2 = "test-line2";
    protected static final String TEST_LINE3 = "test-line3";
    protected static final String TEST_TOWN = "test-town";
    protected static final String TEST_COUNTY = "test-county";
    protected static final String TEST_POSTCODE = "TEST 123";

    protected static final String USERS_ENDPOINT = "/v1/users";
    protected static final String ACCOUNTS_ENDPOINT = "/v1/accounts";
    protected static final String LOGIN_ENDPOINT = "/v1/auth/login";

    protected static final String ACCOUNT_NAME = "Personal Bank Account";
    protected static final String ACCOUNT_TYPE_PERSONAL = "personal";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected AccountRepository accountRepository;

    @Autowired
    protected TransactionRepository transactionRepository;

    @Autowired
    protected RefreshTokenRepository refreshTokenRepository;

    protected final ObjectMapper objectMapper = new ObjectMapper();
    
    @AfterEach
    void cleanUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected String createUserAndGetId(String email) throws Exception {
        CreateUserRequest request = new CreateUserRequest(
                TEST_NAME,
                new AddressDto(TEST_LINE1, null, null, TEST_TOWN, TEST_COUNTY, TEST_POSTCODE),
                TEST_PHONE,
                email);

        MvcResult result = mockMvc.perform(post(USERS_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("id")
                .asText();
    }

    protected String loginAndGetAccessToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email))))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("accessToken")
                .asText();
    }

    protected String loginAndGetRefreshToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email))))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("refreshToken")
                .asText();
    }

    protected String createAccount(String accessToken) throws Exception {
        return createAccount(accessToken, ACCOUNT_NAME);
    }

    protected String createAccount(String accessToken, String name) throws Exception {
        CreateBankAccountRequest request = new CreateBankAccountRequest(name, ACCOUNT_TYPE_PERSONAL);

        MvcResult result = mockMvc.perform(post(ACCOUNTS_ENDPOINT)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("accountNumber")
                .asText();
    }
}
