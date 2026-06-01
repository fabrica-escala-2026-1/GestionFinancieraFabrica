package com.finanzas.gestion_financiera.unit.service;

import com.finanzas.gestion_financiera.entity.Category;
import com.finanzas.gestion_financiera.entity.User;
import com.finanzas.gestion_financiera.repository.CategoryRepository;
import com.finanzas.gestion_financiera.service.CategoryInitService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryInitService - Unit Tests")
class CategoryInitServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryInitService categoryInitService;

    @Captor
    private ArgumentCaptor<List<Category>> categoriesCaptor;

    private User buildUser(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    @Test
    @DisplayName("Should create exactly 10 default categories")
    void shouldCreate10DefaultCategories() {
        categoryInitService.crearCategoriasPorDefecto(buildUser(1L));

        verify(categoryRepository).saveAll(categoriesCaptor.capture());
        assertEquals(10, categoriesCaptor.getValue().size());
    }

    @Test
    @DisplayName("All categories should be associated with the provided user")
    void allShouldBelongToUser() {
        User user = buildUser(5L);
        user.setEmail("owner@email.com");

        categoryInitService.crearCategoriasPorDefecto(user);

        verify(categoryRepository).saveAll(categoriesCaptor.capture());
        boolean allBelongToUser = categoriesCaptor.getValue().stream()
                .allMatch(c -> c.getUsuario().equals(user));
        assertTrue(allBelongToUser);
    }

    @Test
    @DisplayName("Should include all expected category names")
    void shouldIncludeAllExpectedCategories() {
        categoryInitService.crearCategoriasPorDefecto(buildUser(1L));

        verify(categoryRepository).saveAll(categoriesCaptor.capture());
        List<String> names = categoriesCaptor.getValue().stream()
                .map(Category::getNombre)
                .toList();

        List<String> expectedNames = List.of(
                "Salario", "Freelance", "Inversiones", "Otros ingresos",
                "Alimentación", "Transporte", "Vivienda",
                "Salud", "Entretenimiento", "Educación"
        );
        assertTrue(names.containsAll(expectedNames));
    }
}