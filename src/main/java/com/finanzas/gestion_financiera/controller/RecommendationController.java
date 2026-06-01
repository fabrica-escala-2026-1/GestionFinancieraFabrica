package com.finanzas.gestion_financiera.controller;

import com.finanzas.gestion_financiera.dto.RecommendationResponse;
import com.finanzas.gestion_financiera.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@RequestMapping("/api/v1/recomendaciones")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping
    public ResponseEntity<CollectionModel<RecommendationResponse>> getRecommendations() {
        List<RecommendationResponse> lista = recommendationService.getRecommendations();
        lista.forEach(r -> r.add(
                linkTo(methodOn(RecommendationController.class).getRecommendations()).withSelfRel()
        ));
        CollectionModel<RecommendationResponse> collection = CollectionModel.of(
                lista,
                linkTo(methodOn(RecommendationController.class).getRecommendations()).withSelfRel(),
                linkTo(methodOn(BudgetController.class).list()).withRel("presupuestos"),
                linkTo(methodOn(CategoryController.class).listar()).withRel("categorias"),
                linkTo(methodOn(TransactionController.class).listar()).withRel("transacciones")
        );
        return ResponseEntity.ok(collection);
    }
}
