package com.finanzas.gestion_financiera.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank(message = "El nombre de la categoría es obligatorio")
    private String nombre;

}
