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
    class CrearEndpoint {

        @Test
        @DisplayName("Debe crear transacción de ingreso y retornar 200")
        void debeCrearTransaccionIngreso() throws Exception {
            // Arrange
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

            // Act & Assert
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
        @DisplayName("Debe crear transacción de gasto y retornar 200")
        void debeCrearTransaccionGasto() throws Exception {
            // Arrange
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

            // Act & Assert
            mockMvc.perform(post("/api/v1/transacciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tipo").value("GASTO"))
                    .andExpect(jsonPath("$.monto").value(250.75));
        }

        @Test
        @DisplayName("Debe retornar 400 si el tipo es inválido")
        void debeRetornar400SiTipoInvalido() throws Exception {
            // Arrange
            String json = """
                    {
                        "tipo": "INVALIDO",
                        "monto": 100.00,
                        "fecha": "2026-04-01",
                        "categoriaId": 1
                    }
                    """;

            // Act & Assert
            mockMvc.perform(post("/api/v1/transacciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe retornar 400 si el monto es 0 o negativo")
        void debeRetornar400SiMontoInvalido() throws Exception {
            // Arrange
            String json = """
                    {
                        "tipo": "GASTO",
                        "monto": 0,
                        "fecha": "2026-04-01",
                        "categoriaId": 1
                    }
                    """;

            // Act & Assert
            mockMvc.perform(post("/api/v1/transacciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe retornar 400 si falta la categoría")
        void debeRetornar400SiFaltaCategoria() throws Exception {
            // Arrange
            String json = """
                    {
                        "tipo": "INGRESO",
                        "monto": 100.00,
                        "fecha": "2026-04-01"
                    }
                    """;

            // Act & Assert
            mockMvc.perform(post("/api/v1/transacciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/transacciones")
    class ListarEndpoint {

        @Test
        @DisplayName("Debe listar transacciones del usuario autenticado")
        void debeListarTransacciones() throws Exception {
            // Arrange
            List<TransactionResponse> transacciones = List.of(
                    new TransactionResponse(1L, "INGRESO", new BigDecimal("5000"),
                            LocalDate.of(2026, 4, 1), "Salario"),
                    new TransactionResponse(2L, "GASTO", new BigDecimal("200"),
                            LocalDate.of(2026, 4, 2), "Alimentación")
            );
            when(transactionService.listar()).thenReturn(transacciones);

            // Act & Assert
            mockMvc.perform(get("/api/v1/transacciones"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].tipo").value("INGRESO"))
                    .andExpect(jsonPath("$[1].tipo").value("GASTO"));
        }

        @Test
        @DisplayName("Debe retornar lista vacía si no hay transacciones")
        void debeRetornarListaVacia() throws Exception {
            // Arrange
            when(transactionService.listar()).thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/api/v1/transacciones"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }
}
