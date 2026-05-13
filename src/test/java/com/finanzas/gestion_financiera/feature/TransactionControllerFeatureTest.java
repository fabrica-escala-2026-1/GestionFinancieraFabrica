package com.finanzas.gestion_financiera.feature;

import com.finanzas.gestion_financiera.controller.TransactionController;
import com.finanzas.gestion_financiera.dto.TransactionRequest;
import com.finanzas.gestion_financiera.dto.TransactionResponse;
import com.finanzas.gestion_financiera.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Transaction Feature - API /api/v1/transacciones")
class TransactionControllerFeatureTest {

    private MockMvc mockMvc;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransactionController transactionController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(transactionController).build();
    }

    @Nested
    @DisplayName("POST /api/v1/transacciones")
    class CreateEndpoint {

        @Test
        @DisplayName("Should create an INGRESO transaction and return 200")
        void shouldCreateIncomeTransaction() throws Exception {
            TransactionResponse response = new TransactionResponse(
                    1L, "INGRESO", new BigDecimal("5000.00"),
                    LocalDate.of(2026, 4, 1), "Salario");
            when(transactionService.crear(any(TransactionRequest.class))).thenReturn(response);

            String json = """
                    {
                        "tipo": "INGRESO",
                        "monto": 5000.00,
                        "fecha": "2026-04-01",
                        "categoriaId": 1
                    }
                    """;

            mockMvc.perform(post("/api/v1/transacciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.tipo").value("INGRESO"))
                    .andExpect(jsonPath("$.monto").value(5000.00))
                    .andExpect(jsonPath("$.categoria").value("Salario"));
        }

        @Test
        @DisplayName("Should create a GASTO transaction and return 200")
        void shouldCreateExpenseTransaction() throws Exception {
            TransactionResponse response = new TransactionResponse(
                    2L, "GASTO", new BigDecimal("250.75"),
                    LocalDate.of(2026, 4, 5), "Alimentación");
            when(transactionService.crear(any(TransactionRequest.class))).thenReturn(response);

            String json = """
                    {
                        "tipo": "GASTO",
                        "monto": 250.75,
                        "fecha": "2026-04-05",
                        "categoriaId": 5
                    }
                    """;

            mockMvc.perform(post("/api/v1/transacciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tipo").value("GASTO"))
                    .andExpect(jsonPath("$.monto").value(250.75));
        }

        @Test
        @DisplayName("Should return 400 when type is invalid")
        void shouldReturn400WhenTypeIsInvalid() throws Exception {
            String json = """
                    {
                        "tipo": "INVALIDO",
                        "monto": 100.00,
                        "fecha": "2026-04-01",
                        "categoriaId": 1
                    }
                    """;

            mockMvc.perform(post("/api/v1/transacciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when amount is 0 or negative")
        void shouldReturn400WhenAmountIsInvalid() throws Exception {
            String json = """
                    {
                        "tipo": "GASTO",
                        "monto": 0,
                        "fecha": "2026-04-01",
                        "categoriaId": 1
                    }
                    """;

            mockMvc.perform(post("/api/v1/transacciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when category is missing")
        void shouldReturn400WhenCategoryIsMissing() throws Exception {
            String json = """
                    {
                        "tipo": "INGRESO",
                        "monto": 100.00,
                        "fecha": "2026-04-01"
                    }
                    """;

            mockMvc.perform(post("/api/v1/transacciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/transacciones")
    class ListEndpoint {

        @Test
        @DisplayName("Should list transactions of the authenticated user")
        void shouldListTransactions() throws Exception {
            List<TransactionResponse> transactions = List.of(
                    new TransactionResponse(1L, "INGRESO", new BigDecimal("5000"),
                            LocalDate.of(2026, 4, 1), "Salario"),
                    new TransactionResponse(2L, "GASTO", new BigDecimal("200"),
                            LocalDate.of(2026, 4, 2), "Alimentación")
            );
            when(transactionService.listar()).thenReturn(transactions);

            mockMvc.perform(get("/api/v1/transacciones"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].tipo").value("INGRESO"))
                    .andExpect(jsonPath("$[1].tipo").value("GASTO"));
        }

        @Test
        @DisplayName("Should return an empty list when there are no transactions")
        void shouldReturnEmptyList() throws Exception {
            when(transactionService.listar()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/transacciones"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }
}
