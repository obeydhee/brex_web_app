package com.brex.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brex.demo.support.AuthTestSupport;
import com.brex.demo.support.AuthTestSupport.SignedUpUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class UserProfileControllerTest {

    @DynamicPropertySource
    static void sqliteProperties(DynamicPropertyRegistry registry) throws IOException {
        var tempFile = Files.createTempFile("brex-demo-test", ".db");
        tempFile.toFile().deleteOnExit();
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + tempFile);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void signupReturnsTokenAndProfileWithoutPasswordHash() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Jane","lastName":"Doe","email":"jane@example.com",
                                "phoneNumber":"555-0101","paymentName":"jane_doe","password":"secret123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.profile.paymentName").value("jane_doe"))
                .andExpect(jsonPath("$.profile.passwordHash").doesNotExist());
    }

    @Test
    void signupWithDuplicateEmailIsRejected() throws Exception {
        AuthTestSupport.signUp(mockMvc, objectMapper, "first_user");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Other","lastName":"User","email":"first_user@example.com",
                                "phoneNumber":"555-0102","paymentName":"different_name","password":"secret123"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void signupWithDuplicatePaymentNameIsRejected() throws Exception {
        AuthTestSupport.signUp(mockMvc, objectMapper, "taken_name");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Other","lastName":"User","email":"different@example.com",
                                "phoneNumber":"555-0103","paymentName":"taken_name","password":"secret123"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void getCurrentUserWithoutTokenOrUserIdIsRejected() throws Exception {
        // Auth enforcement is currently relaxed (see SecurityConfig): with
        // neither a Bearer token nor a ?userId= fallback, there's simply no
        // way to resolve "the current user", which is a 400, not a 401.
        //
        // Not asserting on the response body here: ResponseStatusException
        // relies on the servlet container's /error forwarding to render its
        // JSON body (with server.error.include-message: always, verified
        // manually against a real running instance), and MockMvc doesn't
        // simulate that container-level forward, so the body comes back
        // empty under test even though it's populated in production.
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCurrentUserWithUserIdFallbackReturnsProfileWithoutAToken() throws Exception {
        SignedUpUser user = AuthTestSupport.signUp(mockMvc, objectMapper, "userid_fallback");

        mockMvc.perform(get("/api/users/me?userId=" + user.userId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentName").value("userid_fallback"));
    }

    @Test
    void getCurrentUserWithValidTokenReturnsProfile() throws Exception {
        SignedUpUser user = AuthTestSupport.signUp(mockMvc, objectMapper, "auth_check");

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentName").value("auth_check"));
    }
}
