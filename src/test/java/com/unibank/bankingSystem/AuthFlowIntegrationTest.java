package com.unibank.bankingSystem;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Full-stack test: real Spring Security filter chain, real BCrypt password hashing,
// real JWT signing/verification, real JPA repositories - backed by the in-memory H2
// database instead of the local MySQL instance. Proves register -> login -> access a
// protected endpoint actually works end to end, and that the account lockout kicks in
// after 5 wrong passwords.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // RabbitMQ auto-config is excluded in tests (see src/test/resources/application.properties),
    // so AuthService's RabbitTemplate dependency needs a stand-in for the context to start.
    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    private String uniqueEmail() {
        return "test-" + UUID.randomUUID() + "@example.com";
    }

    private Map<String, String> registerBody(String email) {
        Map<String, String> body = new HashMap<>();
        body.put("username", "user-" + UUID.randomUUID());
        body.put("email", email);
        body.put("password", "Test1234!");
        body.put("fullName", "Test User");
        return body;
    }

    @Test
    void protectedEndpoint_rejectsRequestsWithNoToken() throws Exception {
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerThenLogin_allowsAccessToProtectedEndpoint() throws Exception {
        String email = uniqueEmail();

        String registerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerBody(email))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(registerResponse).get("token").asText();

        // A brand-new user has no accounts yet, but the request itself must succeed
        // (200, not 401/403), proving the JWT issued at registration is valid.
        mockMvc.perform(get("/api/accounts").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void login_locksAccountAfterFiveWrongPasswords() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerBody(email))))
                .andExpect(status().isOk());

        Map<String, String> wrongLogin = new HashMap<>();
        wrongLogin.put("email", email);
        wrongLogin.put("password", "wrong-password");

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(wrongLogin)))
                    .andExpect(status().isUnauthorized());
        }

        // 6th attempt (even with a correct password) should now be locked out.
        Map<String, String> correctLogin = new HashMap<>();
        correctLogin.put("email", email);
        correctLogin.put("password", "Test1234!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(correctLogin)))
                .andExpect(status().isLocked());
    }
}
