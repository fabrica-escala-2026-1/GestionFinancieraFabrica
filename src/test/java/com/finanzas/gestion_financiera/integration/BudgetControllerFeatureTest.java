package com.finanzas.gestion_financiera.feature;

import com.finanzas.gestion_financiera.controller.BudgetController;
import com.finanzas.gestion_financiera.controller.GlobalExceptionHandlerController;
import com.finanzas.gestion_financiera.dto.BudgetComparisonResponse;
import com.finanzas.gestion_financiera.dto.BudgetRequest;
import com.finanzas.gestion_financiera.dto.BudgetResponse;
import com.finanzas.gestion_financiera.service.BudgetService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Budget Feature - API /api/v1/presupuestos")
class BudgetControllerFeatureTest {

    private MockMvc mockMvc;

    @Mock
    private BudgetService budgetService;

    @InjectMocks
    private BudgetController budgetController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(budgetController)
                .setControllerAdvice(new GlobalExceptionHandlerController())
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/presupuestos")
    class CreateEndpoint {

        @Test
        @DisplayName("Should create budget and return 200")
        void shouldCreateBudget() throws Exception {
            BudgetResponse response = new BudgetResponse(
                    1L, "Alimentación", new BigDecimal("500000"),
                    5, 2026, 2, 7, 2026);
            when(budgetService.create(any(BudgetRequest.class))).thenReturn(response);

            String json = """
                    {
                        "categoryId": 10,
                        "amount": 500000,
                        "durationMonths": 2
                    }
                    """;

            mockMvc.perform(post("/api/v1/presupuestos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.categoryName").value("Alimentación"))
                    .andExpect(jsonPath("$.amount").value(500000))
                    .andExpect(jsonPath("$.startMonth").value(5))
                    .andExpect(jsonPath("$.startYear").value(2026))
                    .andExpect(jsonPath("$.durationMonths").value(2))
                    .andExpect(jsonPath("$.endMonth").value(7))
                    .andExpect(jsonPath("$.endYear").value(2026));
        }

        @Test
        @DisplayName("Should return 400 when category is missing")
        void shouldReturn400WhenCategoryIsMissing() throws Exception {
            String json = """
                    {
                        "amount": 100000,
                        "durationMonths": 1
                    }
                    """;

            mockMvc.perform(post("/api/v1/presupuestos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when amount is 0 or negative")
        void shouldReturn400WhenAmountIsInvalid() throws Exception {
            String json = """
                    {
                        "categoryId": 10,
                        "amount": 0,
                        "durationMonths": 1
                    }
                    """;

            mockMvc.perform(post("/api/v1/presupuestos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when duration exceeds the maximum allowed")
        void shouldReturn400WhenDurationExceedsMaximum() throws Exception {
            String json = """
                    {
                        "categoryId": 10,
                        "amount": 100000,
                        "durationMonths": 13
                    }
                    """;

            mockMvc.perform(post("/api/v1/presupuestos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when duration is negative")
        void shouldReturn400WhenDurationIsNegative() throws Exception {
            String json = """
                    {
                        "categoryId": 10,
                        "amount": 100000,
                        "durationMonths": -1
                    }
                    """;

            mockMvc.perform(post("/api/v1/presupuestos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when duration is missing")
        void shouldReturn400WhenDurationIsMissing() throws Exception {
            String json = """
                    {
                        "categoryId": 10,
                        "amount": 100000
                    }
                    """;

            mockMvc.perform(post("/api/v1/presupuestos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when service throws a business error")
        void shouldReturn400WhenServiceThrowsBusinessError() throws Exception {
            when(budgetService.create(any(BudgetRequest.class)))
                    .thenThrow(new RuntimeException(
                            "Ya existe un presupuesto activo para esta categoría en ese período"));

            String json = """
                    {
                        "categoryId": 10,
                        "amount": 100000,
                        "durationMonths": 1
                    }
                    """;

            mockMvc.perform(post("/api/v1/presupuestos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.mensaje")
                            .value("Ya existe un presupuesto activo para esta categoría en ese período"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/presupuestos")
    class ListEndpoint {

        @Test
        @DisplayName("Should list budgets for the authenticated user")
        void shouldListBudgets() throws Exception {
            List<BudgetResponse> budgets = List.of(
                    new BudgetResponse(1L, "Alimentación", new BigDecimal("300000"),
                            5, 2026, 1, 6, 2026),
                    new BudgetResponse(2L, "Transporte", new BigDecimal("150000"),
                            5, 2026, 0, 5, 2026)
            );
            when(budgetService.list()).thenReturn(budgets);

            mockMvc.perform(get("/api/v1/presupuestos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].categoryName").value("Alimentación"))
                    .andExpect(jsonPath("$[1].categoryName").value("Transporte"));
        }

        @Test
        @DisplayName("Should return an empty list when there are no budgets")
        void shouldReturnEmptyList() throws Exception {
            when(budgetService.list()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/presupuestos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/presupuestos/{id}")
    class UpdateEndpoint {

        @Test
        @DisplayName("Should update an existing budget")
        void shouldUpdateBudget() throws Exception {
            BudgetResponse response = new BudgetResponse(
                    3L, "Alimentación", new BigDecimal("250000"),
                    5, 2026, 4, 9, 2026);
            when(budgetService.update(eq(3L), any(BudgetRequest.class))).thenReturn(response);

            String json = """
                    {
                        "categoryId": 10,
                        "amount": 250000,
                        "durationMonths": 4
                    }
                    """;

            mockMvc.perform(put("/api/v1/presupuestos/3")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(3))
                    .andExpect(jsonPath("$.amount").value(250000))
                    .andExpect(jsonPath("$.durationMonths").value(4));
        }

        @Test
        @DisplayName("Should return 400 when update payload is invalid")
        void shouldReturn400WhenPayloadIsInvalid() throws Exception {
            String json = """
                    {
                        "categoryId": null,
                        "amount": -10,
                        "durationMonths": 100
                    }
                    """;

            mockMvc.perform(put("/api/v1/presupuestos/3")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when budget does not exist")
        void shouldReturn400WhenBudgetNotFound() throws Exception {
            when(budgetService.update(eq(99L), any(BudgetRequest.class)))
                    .thenThrow(new RuntimeException("Presupuesto no encontrado"));

            String json = """
                    {
                        "categoryId": 10,
                        "amount": 100000,
                        "durationMonths": 1
                    }
                    """;

            mockMvc.perform(put("/api/v1/presupuestos/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.mensaje").value("Presupuesto no encontrado"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/presupuestos/{id}")
    class DeleteEndpoint {

        @Test
        @DisplayName("Should delete budget and return 204")
        void shouldDeleteBudget() throws Exception {
            doNothing().when(budgetService).delete(4L);

            mockMvc.perform(delete("/api/v1/presupuestos/4"))
                    .andExpect(status().isNoContent());
            verify(budgetService).delete(4L);
        }

        @Test
        @DisplayName("Should return 400 when budget does not exist")
        void shouldReturn400WhenBudgetNotFound() throws Exception {
            doThrow(new RuntimeException("Presupuesto no encontrado"))
                    .when(budgetService).delete(99L);

            mockMvc.perform(delete("/api/v1/presupuestos/99"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.mensaje").value("Presupuesto no encontrado"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/presupuestos/comparativa")
    class ComparativaEndpoint {

        @Test
        @DisplayName("Should return comparison covering all scenarios")
        void shouldReturnComparison() throws Exception {
            List<BudgetComparisonResponse> comparison = List.of(
                    new BudgetComparisonResponse(
                            "Alimentación", new BigDecimal("100000"),
                            new BigDecimal("50000"), new BigDecimal("50000"),
                            50.0, "Llevas el 50.0% del presupuesto de Alimentación utilizado"),
                    new BudgetComparisonResponse(
                            "Transporte", null, new BigDecimal("20000"), null, null, null)
            );
            when(budgetService.comparativa()).thenReturn(comparison);

            mockMvc.perform(get("/api/v1/presupuestos/comparativa"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[0].categoriaNombre").value("Alimentación"))
                    .andExpect(jsonPath("$.content[0].porcentaje").value(50.0))
                    .andExpect(jsonPath("$.content[0].presupuesto").value(100000))
                    .andExpect(jsonPath("$.content[0].gastado").value(50000))
                    .andExpect(jsonPath("$.content[1].categoriaNombre").value("Transporte"))
                    .andExpect(jsonPath("$.content[1].presupuesto").doesNotExist())
                    .andExpect(jsonPath("$.content[1].porcentaje").doesNotExist());
        }

        @Test
        @DisplayName("Should return empty comparison when there are no expense categories")
        void shouldReturnEmptyComparison() throws Exception {
            when(budgetService.comparativa()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/presupuestos/comparativa"))
                    .andExpect(status().isOk());
        }
    }
}
