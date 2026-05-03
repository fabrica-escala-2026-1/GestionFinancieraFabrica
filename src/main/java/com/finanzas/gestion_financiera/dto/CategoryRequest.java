package com.finanzas.gestion_financiera.dto;

import com.finanzas.gestion_financiera.entity.Category.TipoCategoria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank(message = "El nombre de la categoría es obligatorio")
    private String nombre;

    @NotNull(message = "El tipo es obligatorio, debe ser INGRESO o GASTO")
    private TipoCategoria tipo;
}
