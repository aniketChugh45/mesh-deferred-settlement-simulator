package com.demo.upimesh;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiValidationIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void healthEndpointReportsApplicationUp() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void demoSendRejectsInvalidAmountWithStructuredError() throws Exception {
        mockMvc.perform(post("/api/demo/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "senderVpa": "user1@demo",
                                  "receiverVpa": "user2@demo",
                                  "amount": -1,
                                  "pin": "1234"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.violations[0].field").value("amount"));
    }
}
