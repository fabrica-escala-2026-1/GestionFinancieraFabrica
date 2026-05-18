package com.finanzas.gestion_financiera.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class CategoryResponse extends RepresentationModel<CategoryResponse>{

    private Long id;
    private String nombre;

}
