package com.finanzas.gestion_financiera.feature;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finanzas.gestion_financiera.dto.TransactionRequest;
import com.finanzas.gestion_financiera.dto.TransactionResponse;
import com.finanzas.gestion_financiera.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Transaction Feature - API /api/v1/transacciones")
class TransactionControllerFeatureTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    @Nested
    @DisplayName("POST /api/v1/transacciones")
    @WithMockUser(username = "test@email.com", roles = "USER")
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
        @DisplayName("Debe retornar 400 si falta el tipo")
        void debeRetornar400SiFaltaTipo() throws Exception {
            // Arrange
            String json = """
                    {
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
        @DisplayName("Debe retornar 400 si falta el monto")
        void debeRetornar400SiFaltaMonto() throws Exception {
            // Arrange
            String json = """
                    {
                        "tipo": "GASTO",
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
        @DisplayName("Debe retornar 400 si el JSON es inválido")
        void debeRetornar400SiJsonInvalido() throws Exception {
            // Arrange
            String json = """
                    {
                        "tipo": "GASTO",
                        "monto": "no-es-numero",
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
        @DisplayName("Debe retornar 400 si el body está vacío")
        void debeRetornar400SiBodyVacio() throws Exception {
            // Arrange
            String json = "{}";

            // Act & Assert
            mockMvc.perform(post("/api/v1/transacciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe retornar 400 si la categoría no existe")
        void debeRetornar400SiCategoriaNoExiste() throws Exception {
            // Arrange
            when(transactionService.crear(any(TransactionRequest.class)))
                    .thenThrow(new jakarta.persistence.EntityNotFoundException("Categoría no válida"));

            String json = """
                    {
                        "tipo": "GASTO",
                        "monto": 100.00,
                        "fecha": "2026-04-01",
                        "categoriaId": 999
                    }
                    """;

            // Act & Assert
            mockMvc.perform(post("/api/v1/transacciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe retornar 400 si el usuario no está autenticado correctamente")
        void debeRetornar400SiUsuarioNoAutenticado() throws Exception {
            // Arrange
            when(transactionService.crear(any(TransactionRequest.class)))
                    .thenThrow(new jakarta.persistence.EntityNotFoundException("Usuario no encontrado"));

            String json = """
                    {
                        "tipo": "INGRESO",
                        "monto": 500.00,
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
        @DisplayName("Debe retornar 400 si el monto es negativo")
        void debeRetornar400SiMontoNegativo() throws Exception {
            // Arrange
            when(transactionService.crear(any(TransactionRequest.class)))
                    .thenThrow(new IllegalArgumentException("Debes ingresar un monto válido"));

            String json = """
                    {
                        "tipo": "GASTO",
                        "monto": -50.00,
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
        @DisplayName("Debe retornar 400 si el tipo no es INGRESO o GASTO")
        void debeRetornar400SiTipoNoEsIngresoOGasto() throws Exception {
            // Arrange
            when(transactionService.crear(any(TransactionRequest.class)))
                    .thenThrow(new IllegalArgumentException("El tipo debe ser INGRESO o GASTO"));

            String json = """
                    {
                        "tipo": "TRANSFERENCIA",
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
    }

    @Nested
    @DisplayName("GET /api/v1/transacciones")
    @WithMockUser(username = "test@email.com", roles = "USER")
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

        @Test
        @DisplayName("Debe retornar 400 si hay error al obtener usuario autenticado")
        void debeRetornar400SiErrorUsuarioAutenticado() throws Exception {
            // Arrange
            when(transactionService.listar())
                    .thenThrow(new jakarta.persistence.EntityNotFoundException("Usuario no encontrado"));

            // Act & Assert
            mockMvc.perform(get("/api/v1/transacciones"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe listar transacciones con diferentes tipos y montos")
        void debeListarTransaccionesDiversas() throws Exception {
            // Arrange
            List<TransactionResponse> transacciones = List.of(
                    new TransactionResponse(1L, "INGRESO", new BigDecimal("5000.00"),
                            LocalDate.of(2026, 4, 1), "Salario"),
                    new TransactionResponse(2L, "GASTO", new BigDecimal("250.75"),
                            LocalDate.of(2026, 4, 5), "Alimentación"),
                    new TransactionResponse(3L, "GASTO", new BigDecimal("1500.00"),
                            LocalDate.of(2026, 4, 10), "Transporte"),
                    new TransactionResponse(4L, "INGRESO", new BigDecimal("1200.00"),
                            LocalDate.of(2026, 4, 15), "Freelance")
            );
            when(transactionService.listar()).thenReturn(transacciones);

            // Act & Assert
            mockMvc.perform(get("/api/v1/transacciones"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(4))
                    .andExpect(jsonPath("$[0].tipo").value("INGRESO"))
                    .andExpect(jsonPath("$[0].monto").value(5000.00))
                    .andExpect(jsonPath("$[1].tipo").value("GASTO"))
                    .andExpect(jsonPath("$[1].monto").value(250.75))
                    .andExpect(jsonPath("$[2].tipo").value("GASTO"))
                    .andExpect(jsonPath("$[2].monto").value(1500.00))
                    .andExpect(jsonPath("$[3].tipo").value("INGRESO"))
                    .andExpect(jsonPath("$[3].monto").value(1200.00));
        }
    }
}
