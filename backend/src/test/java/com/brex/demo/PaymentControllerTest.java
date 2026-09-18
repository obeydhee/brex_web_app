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
class PaymentControllerTest {

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
    void successfulTransferMovesBalancesAndRecordsSuccess() throws Exception {
        SignedUpUser sender = AuthTestSupport.signUp(mockMvc, objectMapper, "pay_sender");
        SignedUpUser receiver = AuthTestSupport.signUp(mockMvc, objectMapper, "pay_receiver");
        deposit(sender.token(), "100.00");

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + sender.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverPaymentName\":\"pay_receiver\",\"amount\":40.00,\"note\":\"lunch\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.senderPaymentName").value("pay_sender"))
                .andExpect(jsonPath("$.receiverPaymentName").value("pay_receiver"))
                .andExpect(jsonPath("$.amount").value(40.00));

        mockMvc.perform(get("/api/accounts/me").header("Authorization", "Bearer " + sender.token()))
                .andExpect(jsonPath("$.balance").value(60.00));
        mockMvc.perform(get("/api/accounts/me").header("Authorization", "Bearer " + receiver.token()))
                .andExpect(jsonPath("$.balance").value(40.00));
    }

    @Test
    void insufficientFundsRecordsFailureTransactionAndReturns201() throws Exception {
        SignedUpUser sender = AuthTestSupport.signUp(mockMvc, objectMapper, "poor_sender");
        AuthTestSupport.signUp(mockMvc, objectMapper, "lucky_receiver");
        // sender has a zero balance — no deposit

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + sender.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverPaymentName\":\"lucky_receiver\",\"amount\":50.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FAILURE"));

        mockMvc.perform(get("/api/accounts/me").header("Authorization", "Bearer " + sender.token()))
                .andExpect(jsonPath("$.balance").value(0));
    }

    @Test
    void selfPaymentIsRejected() throws Exception {
        SignedUpUser user = AuthTestSupport.signUp(mockMvc, objectMapper, "self_payer");
        deposit(user.token(), "50.00");

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverPaymentName\":\"self_payer\",\"amount\":10.00}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void payingUnknownReceiverIsNotFound() throws Exception {
        SignedUpUser user = AuthTestSupport.signUp(mockMvc, objectMapper, "confused_sender");
        deposit(user.token(), "50.00");

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverPaymentName\":\"does_not_exist\",\"amount\":10.00}"))
                .andExpect(status().isNotFound());
    }

    private void deposit(String token, String amount) throws Exception {
        mockMvc.perform(post("/api/accounts/me/deposit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":" + amount + "}"))
                .andExpect(status().isOk());
    }
}
