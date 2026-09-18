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
class AccountControllerTest {

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
    void newAccountStartsAtZeroBalance() throws Exception {
        SignedUpUser user = AuthTestSupport.signUp(mockMvc, objectMapper, "zero_balance");

        mockMvc.perform(get("/api/accounts/me").header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(0));
    }

    @Test
    void depositIncreasesBalanceAndAppendsLedgerEntry() throws Exception {
        SignedUpUser user = AuthTestSupport.signUp(mockMvc, objectMapper, "depositor");

        mockMvc.perform(post("/api/accounts/me/deposit")
                        .header("Authorization", "Bearer " + user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(100.00));

        mockMvc.perform(get("/api/accounts/me/ledger").header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].entryType").value("DEPOSIT"))
                .andExpect(jsonPath("$.content[0].amount").value(100.00))
                .andExpect(jsonPath("$.content[0].balanceAfter").value(100.00));
    }

    @Test
    void withdrawWithSufficientFundsSucceeds() throws Exception {
        SignedUpUser user = AuthTestSupport.signUp(mockMvc, objectMapper, "withdrawer");
        deposit(user.token(), "50.00");

        mockMvc.perform(post("/api/accounts/me/withdraw")
                        .header("Authorization", "Bearer " + user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":20.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(30.00));
    }

    @Test
    void withdrawWithInsufficientFundsIsRejectedAndBalanceUnchanged() throws Exception {
        SignedUpUser user = AuthTestSupport.signUp(mockMvc, objectMapper, "broke_withdrawer");
        deposit(user.token(), "10.00");

        mockMvc.perform(post("/api/accounts/me/withdraw")
                        .header("Authorization", "Bearer " + user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":25.00}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/accounts/me").header("Authorization", "Bearer " + user.token()))
                .andExpect(jsonPath("$.balance").value(10.00));

        mockMvc.perform(get("/api/accounts/me/ledger").header("Authorization", "Bearer " + user.token()))
                .andExpect(jsonPath("$.content.length()").value(1)); // only the earlier deposit
    }

    private void deposit(String token, String amount) throws Exception {
        mockMvc.perform(post("/api/accounts/me/deposit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":" + amount + "}"))
                .andExpect(status().isOk());
    }
}
