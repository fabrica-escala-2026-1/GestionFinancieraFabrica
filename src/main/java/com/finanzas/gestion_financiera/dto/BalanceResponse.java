package com.finanzas.gestion_financiera.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class BalanceResponse extends RepresentationModel<BalanceResponse> {

    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    private BigDecimal ingresosTotales;
    private BigDecimal gastosTotales;
    private BigDecimal balanceNeto;

    private Boolean balanceNegativo;
    private String mensaje;
}