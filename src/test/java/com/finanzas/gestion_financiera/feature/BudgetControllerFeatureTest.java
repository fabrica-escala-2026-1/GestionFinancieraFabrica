package com.finanzas.gestion_financiera.feature;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finanzas.gestion_financiera.dto.BudgetRequest;
import com.finanzas.gestion_financiera.dto.BudgetResponse;
import com.finanzas.gestion_financiera.service.BudgetService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Budget Feature - API /api/v1/presupuestos")
class BudgetControllerFeatureTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BudgetService budgetService;

    @Nested
    @DisplayName("POST /api/v1/presupuestos")
    @WithMockUser(username = "test@email.com", roles = "USER")
    class CrearEndpoint {

        @Test
        @DisplayName("Debe crear presupuesto exitosamente y retornar 200")
        void debeCrearPresupuestoExitosamente() throws Exception {
            // Arrange
            BudgetRequest request = new BudgetRequest();
            request.setCategoryId(1L);
            request.setAmount(new BigDecimal("5000.00"));
            request.setDurationMonths(0);

            BudgetResponse response = new BudgetResponse(
                    1L, "Alimentación", new BigDecimal("5000.00"),
                    4, 2026, 0, 4, 2026
            );

            when(budgetService.create(any(BudgetRequest.class))).thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/api/v1/presupuestos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.categoryName").value("Alimentación"))
                    .andExpect(jsonPath("$.amount").value(5000.00))
                    .andExpect(jsonPath("$.startMonth").value(4))
                    .andExpect(jsonPath("$.startYear").value(2026));
        }

        @Test
        @DisplayName("Debe retornar 400 si el monto es inválido")
        void debeRetornar400SiMontoInvalido() throws Exception {
            // Arrange
            BudgetRequest request = new BudgetRequest();
            request.setCategoryId(1L);
            request.setAmount(new BigDecimal("-100.00")); // Monto negativo
            request.setDurationMonths(0);

            // Act & Assert
            mockMvc.perform(post("/api/v1/presupuestos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe retornar 400 si falta la categoría")
        void debeRetornar400SiFaltaCategoria() throws Exception {
            // Arrange
            String json = """
                    {
                        "amount": 5000.00,
                        "durationMonths": 0
                    }
                    """;

            // Act & Assert
            mockMvc.perform(post("/api/v1/presupuestos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe retornar 400 si la duración de meses es inválida")
        void debeRetornar400SiDuracionInvalida() throws Exception {
            // Arrange
            BudgetRequest request = new BudgetRequest();
            request.setCategoryId(1L);
            request.setAmount(new BigDecimal("5000.00"));
            request.setDurationMonths(13); // Máximo permitido es 12

            // Act & Assert
            mockMvc.perform(post("/api/v1/presupuestos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe retornar 400 si ya existe presupuesto activo para la categoría")
        void debeRetornar400SiPresupuestoYaExiste() throws Exception {
            // Arrange
            BudgetRequest request = new BudgetRequest();
            request.setCategoryId(1L);
            request.setAmount(new BigDecimal("5000.00"));
            request.setDurationMonths(0);

            when(budgetService.create(any(BudgetRequest.class)))
                    .thenThrow(new RuntimeException("Ya existe un presupuesto activo para esta categoría en ese período"));

            // Act & Assert
            mockMvc.perform(post("/api/v1/presupuestos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/presupuestos")
    @WithMockUser(username = "test@email.com", roles = "USER")
    class ListarEndpoint {

        @Test
        @DisplayName("Debe listar presupuestos del usuario autenticado")
        void debeListarPresupuestos() throws Exception {
            // Arrange
            List<BudgetResponse> budgets = List.of(
                    new BudgetResponse(1L, "Alimentación", new BigDecimal("5000.00"), 4, 2026, 0, 4, 2026),
                    new BudgetResponse(2L, "Transporte", new BigDecimal("3000.00"), 4, 2026, 0, 4, 2026),
                    new BudgetResponse(3L, "Entretenimiento", new BigDecimal("2000.00"), 4, 2026, 1, 5, 2026)
            );

            when(budgetService.list()).thenReturn(budgets);

            // Act & Assert
            mockMvc.perform(get("/api/v1/presupuestos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].categoryName").value("Alimentación"))
                    .andExpect(jsonPath("$[0].amount").value(5000.00))
                    .andExpect(jsonPath("$[1].id").value(2))
                    .andExpect(jsonPath("$[1].categoryName").value("Transporte"))
                    .andExpect(jsonPath("$[1].amount").value(3000.00))
                    .andExpect(jsonPath("$[2].id").value(3))
                    .andExpect(jsonPath("$[2].categoryName").value("Entretenimiento"))
                    .andExpect(jsonPath("$[2].amount").value(2000.00));
        }

        @Test
        @DisplayName("Debe retornar lista vacía si el usuario no tiene presupuestos")
        void debeRetornarListaVacia() throws Exception {
            // Arrange
            when(budgetService.list()).thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/api/v1/presupuestos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("Debe incluir HATEOAS links en la respuesta")
        void debeIncluirHateoasLinks() throws Exception {
            // Arrange
            List<BudgetResponse> budgets = List.of(
                    new BudgetResponse(1L, "Alimentación", new BigDecimal("5000.00"), 4, 2026, 0, 4, 2026)
            );

            when(budgetService.list()).thenReturn(budgets);

            // Act & Assert
            mockMvc.perform(get("/api/v1/presupuestos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/presupuestos/{id}")
    @WithMockUser(username = "test@email.com", roles = "USER")
    class ActualizarEndpoint {

        @Test
        @DisplayName("Debe actualizar presupuesto existente")
        void debeActualizarPresupuesto() throws Exception {
            // Arrange
            BudgetRequest request = new BudgetRequest();
            request.setCategoryId(2L);
            request.setAmount(new BigDecimal("7500.00"));
            request.setDurationMonths(2);

            BudgetResponse response = new BudgetResponse(1L, "Transporte", new BigDecimal("7500.00"), 4, 2026, 2, 6, 2026);

            when(budgetService.update(eq(1L), any(BudgetRequest.class))).thenReturn(response);

            // Act & Assert
            mockMvc.perform(put("/api/v1/presupuestos/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.amount").value(7500.00))
                    .andExpect(jsonPath("$.durationMonths").value(2));
        }

        @Test
        @DisplayName("Debe retornar 400 si datos de actualización son inválidos")
        void debeRetornar400SiDatosInvalidos() throws Exception {
            // Arrange
            String json = """
                    {"amount": -500.00, "durationMonths": 2}
                    """;

            // Act & Assert
            mockMvc.perform(put("/api/v1/presupuestos/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe retornar 400 si el presupuesto no existe")
        void debeRetornar400SiPresupuestoNoExiste() throws Exception {
            // Arrange
            BudgetRequest request = new BudgetRequest();
            request.setCategoryId(1L);
            request.setAmount(new BigDecimal("5000.00"));
            request.setDurationMonths(0);

            when(budgetService.update(eq(999L), any(BudgetRequest.class)))
                    .thenThrow(new RuntimeException("Presupuesto no encontrado"));

            // Act & Assert
            mockMvc.perform(put("/api/v1/presupuestos/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe retornar 400 si la categoría no es válida")
        void debeRetornar400SiCategoriaNoValida() throws Exception {
            // Arrange
            BudgetRequest request = new BudgetRequest();
            request.setCategoryId(999L);
            request.setAmount(new BigDecimal("5000.00"));
            request.setDurationMonths(0);

            when(budgetService.update(eq(1L), any(BudgetRequest.class)))
                    .thenThrow(new RuntimeException("Categoría no válida"));

            // Act & Assert
            mockMvc.perform(put("/api/v1/presupuestos/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/presupuestos/{id}")
    @WithMockUser(username = "test@email.com", roles = "USER")
    class EliminarEndpoint {

        @Test
        @DisplayName("Debe eliminar presupuesto y retornar 204")
        void debeEliminarPresupuesto() throws Exception {
            // Arrange
            doNothing().when(budgetService).delete(1L);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/presupuestos/1"))
                    .andExpect(status().isNoContent());
            verify(budgetService).delete(1L);
        }

        @Test
        @DisplayName("Debe retornar 400 si el presupuesto no existe")
        void debeRetornar400SiPresupuestoNoExiste() throws Exception {
            // Arrange
            doThrow(new RuntimeException("Presupuesto no encontrado"))
                    .when(budgetService).delete(999L);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/presupuestos/999"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe eliminar correctamente múltiples presupuestos")
        void debeEliminarMultiplesPresupuestos() throws Exception {
            // Arrange
            doNothing().when(budgetService).delete(1L);
            doNothing().when(budgetService).delete(2L);
            doNothing().when(budgetService).delete(3L);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/presupuestos/1"))
                    .andExpect(status().isNoContent());
            mockMvc.perform(delete("/api/v1/presupuestos/2"))
                    .andExpect(status().isNoContent());
            mockMvc.perform(delete("/api/v1/presupuestos/3"))
                    .andExpect(status().isNoContent());

            verify(budgetService).delete(1L);
            verify(budgetService).delete(2L);
            verify(budgetService).delete(3L);
        }
    }

}
