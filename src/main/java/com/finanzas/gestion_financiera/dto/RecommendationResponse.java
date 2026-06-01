package com.finanzas.gestion_financiera.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;

import java.util.List;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class RecommendationResponse extends RepresentationModel<RecommendationResponse> {
    private String tipo;        // AHORRO, EXCESO_GASTO, BALANCE_NEGATIVO, SIN_PRESUPUESTO, SIN_DATOS
    private String mensaje;
    private List<String> categorias; // null si no aplica
}