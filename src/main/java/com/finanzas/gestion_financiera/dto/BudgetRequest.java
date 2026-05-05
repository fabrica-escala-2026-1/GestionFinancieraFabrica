package com.finanzas.gestion_financiera.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BudgetRequest {

    @NotNull(message = "La categoría es obligatoria")
    private Long categoryId;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal amount;

    // 0 = solo el mes actual, máximo 12 meses adicionales
    @NotNull(message = "La duración es obligatoria")
    @Min(value = 0, message = "La duración mínima es 0 meses")
    @Max(value = 12, message = "La duración máxima es 12 meses")
    private Integer durationMonths;

    // startMonth y startYear no se reciben — se asignan automáticamente
}