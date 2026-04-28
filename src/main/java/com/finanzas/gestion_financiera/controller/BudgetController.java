package com.finanzas.gestion_financiera.controller;

import com.finanzas.gestion_financiera.dto.BudgetRequest;
import com.finanzas.gestion_financiera.dto.BudgetResponse;
import com.finanzas.gestion_financiera.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@RequestMapping("/api/v1/presupuestos")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    public ResponseEntity<BudgetResponse> create(
            @Valid @RequestBody BudgetRequest request) {

        BudgetResponse response = budgetService.create(request);
        response.add(
                linkTo(methodOn(BudgetController.class).list()).withRel("all-budgets"),
                linkTo(methodOn(BudgetController.class).update(response.getId(), request)).withRel("update"),
                linkTo(methodOn(BudgetController.class).delete(response.getId())).withRel("delete")
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<BudgetResponse>> list() {

        List<BudgetResponse> budgets = budgetService.list();
        budgets.forEach(b -> b.add(
                linkTo(methodOn(BudgetController.class).update(b.getId(), null)).withRel("update"),
                linkTo(methodOn(BudgetController.class).delete(b.getId())).withRel("delete")
        ));
        return ResponseEntity.ok(budgets);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BudgetResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody BudgetRequest request) {

        BudgetResponse response = budgetService.update(id, request);
        response.add(
                linkTo(methodOn(BudgetController.class).list()).withRel("all-budgets"),
                linkTo(methodOn(BudgetController.class).delete(response.getId())).withRel("delete")
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        budgetService.delete(id);
        return ResponseEntity.noContent().build();
    }
}