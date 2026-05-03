package com.finanzas.gestion_financiera.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.hateoas.RepresentationModel;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class TransactionResponse extends RepresentationModel<TransactionResponse> {
    private Long id;
    private String tipo;
    private BigDecimal monto;
    private LocalDate fecha;
    private String categoria;
}