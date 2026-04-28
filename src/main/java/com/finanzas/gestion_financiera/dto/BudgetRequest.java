package com.finanzas.gestion_financiera.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BudgetRequest {

    @NotNull(message = "La categoría es obligatoria")
    private Long categoryId;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal amount;

    @NotNull(message = "La fecha final es obligatoria")
    @Future(message = "La fecha final debe ser una fecha futura")
    private LocalDate endDate;

    // startDate no se recibe — se asigna automáticamente en la entidad
}