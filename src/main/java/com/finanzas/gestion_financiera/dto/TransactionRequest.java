package com.finanzas.gestion_financiera.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TransactionRequest {

    @NotBlank(message = "El tipo es obligatorio")
    @Pattern(
            regexp = "INGRESO|GASTO",
            message = "El tipo debe ser INGRESO o GASTO"
    )
    private String tipo;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "Debes ingresar un monto válido")
    private BigDecimal monto;

    @NotNull(message = "La categoría es obligatoria")
    private Long categoriaId;

    // fecha eliminada — se asigna automáticamente en el service
}