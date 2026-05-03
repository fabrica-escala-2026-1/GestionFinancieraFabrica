package com.finanzas.gestion_financiera.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ResumenGastosMensualesResponse {
    private Integer anio;
    private Integer mes;
    private String mensaje;
    private List<GastoPorCategoriaResponse> categorias;
}