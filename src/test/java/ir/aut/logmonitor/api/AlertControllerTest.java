package ir.aut.logmonitor.api;

import ir.aut.logmonitor.alert.Alert;
import ir.aut.logmonitor.alert.AlertRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer test for AlertController. @WebMvcTest starts only the web
 * layer (not the full app / not a real database), and AlertRepository is
 * mocked with @MockitoBean so we can control exactly what it returns.
 */
//@WebMvcTest(AlertController.class)
class AlertControllerTest {

//    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AlertRepository alertRepository;

    @Test
    void returnsAlertsOrderedByTimeAsJson() throws Exception {
        Alert older = new Alert("error-log-rule", "auth-service", "Something broke",
                LocalDateTime.of(2025, 7, 19, 10, 0));
        Alert newer = new Alert("warning-rate-rule", "billing-service", "High warning rate",
                LocalDateTime.of(2025, 7, 19, 11, 0));

        // Repository already returns them in "most recent first" order —
        // the controller shouldn't need to re-sort, just pass it through.
        when(alertRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(newer, older));

        mockMvc.perform(get("/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].ruleName").value("warning-rate-rule"))
                .andExpect(jsonPath("$[0].componentName").value("billing-service"))
                .andExpect(jsonPath("$[1].ruleName").value("error-log-rule"));
    }

    @Test
    void returnsEmptyArrayWhenNoAlertsExist() throws Exception {
        when(alertRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        mockMvc.perform(get("/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}