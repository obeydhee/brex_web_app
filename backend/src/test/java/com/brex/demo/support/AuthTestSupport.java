package com.brex.demo.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Shared helper for tests that need an authenticated user: signs a fresh user
 * up via the real /api/users endpoint and extracts the resulting JWT + id.
 */
public final class AuthTestSupport {

    private AuthTestSupport() {
    }

    public record SignedUpUser(String token, Long userId, String paymentName) {
    }

    public static SignedUpUser signUp(MockMvc mockMvc, ObjectMapper mapper, String paymentName) throws Exception {
        String body = """
                {"firstName":"Test","lastName":"User","email":"%s@example.com",
                "phoneNumber":"555-0100","paymentName":"%s","password":"password123"}
                """.formatted(paymentName, paymentName);
        MvcResult result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = mapper.readTree(result.getResponse().getContentAsString());
        String token = json.get("token").asText();
        Long userId = json.get("profile").get("id").asLong();
        return new SignedUpUser(token, userId, paymentName);
    }
}
