package com.brex.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brex.demo.support.AuthTestSupport;
import com.brex.demo.support.AuthTestSupport.SignedUpUser;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerTest {

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
    void historyDefaultsToOneWeekWindowAndIsPaginated() throws Exception {
        SignedUpUser sender = AuthTestSupport.signUp(mockMvc, objectMapper, "history_sender");
        SignedUpUser receiver = AuthTestSupport.signUp(mockMvc, objectMapper, "history_receiver");
        deposit(sender.token(), "1000.00");

        for (int i = 0; i < 3; i++) {
            pay(sender.token(), "history_receiver", "1.00");
        }

        mockMvc.perform(get("/api/transactions/me").header("Authorization", "Bearer " + sender.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void invalidWindowIsRejected() throws Exception {
        SignedUpUser user = AuthTestSupport.signUp(mockMvc, objectMapper, "bad_window_user");

        mockMvc.perform(get("/api/transactions/me?window=1Y")
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void gettingTransactionAsNonPartyIsForbidden() throws Exception {
        SignedUpUser sender = AuthTestSupport.signUp(mockMvc, objectMapper, "tx_sender");
        AuthTestSupport.signUp(mockMvc, objectMapper, "tx_receiver");
        SignedUpUser bystander = AuthTestSupport.signUp(mockMvc, objectMapper, "tx_bystander");
        deposit(sender.token(), "50.00");

        Long txId = pay(sender.token(), "tx_receiver", "10.00");

        mockMvc.perform(get("/api/transactions/" + txId).header("Authorization", "Bearer " + bystander.token()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/transactions/" + txId).header("Authorization", "Bearer " + sender.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(txId));
    }

    private void deposit(String token, String amount) throws Exception {
        mockMvc.perform(post("/api/accounts/me/deposit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":" + amount + "}"))
                .andExpect(status().isOk());
    }

    private Long pay(String token, String receiverPaymentName, String amount) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverPaymentName\":\"" + receiverPaymentName + "\",\"amount\":" + amount + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("id").asLong();
    }
}
