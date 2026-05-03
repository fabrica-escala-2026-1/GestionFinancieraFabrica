package com.finanzas.gestion_financiera.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class BudgetComparisonResponse extends RepresentationModel<BudgetComparisonResponse>{
    private String categoriaNombre;
    private BigDecimal presupuesto;   // null si no tiene presupuesto
    private BigDecimal gastado;
    private BigDecimal disponible;    // null si no tiene presupuesto
    private Double porcentaje;        // null si no tiene presupuesto
    private String alerta;
}
