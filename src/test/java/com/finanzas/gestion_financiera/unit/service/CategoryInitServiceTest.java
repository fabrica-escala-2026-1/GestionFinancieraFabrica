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

    @Test
    @DisplayName("Should create exactly 10 default categories")
    void shouldCreate10DefaultCategories() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@email.com");

        categoryInitService.crearCategoriasPorDefecto(user);

        verify(categoryRepository).saveAll(categoriesCaptor.capture());
        List<Category> categories = categoriesCaptor.getValue();
        assertEquals(10, categories.size());
    }

    @Test
    @DisplayName("Should create the 4 expected income categories")
    void shouldCreate4IngresoCategories() {
        User user = new User();
        user.setId(1L);

        categoryInitService.crearCategoriasPorDefecto(user);

        verify(categoryRepository).saveAll(categoriesCaptor.capture());
        List<String> incomeNames = List.of("Salario", "Freelance", "Inversiones", "Otros ingresos");
        long incomeCount = categoriesCaptor.getValue().stream()
                .filter(c -> incomeNames.contains(c.getNombre()))
                .count();
        assertEquals(4, incomeCount);
    }

    @Test
    @DisplayName("Should create the 6 expected expense categories")
    void shouldCreate6GastoCategories() {
        User user = new User();
        user.setId(1L);

        categoryInitService.crearCategoriasPorDefecto(user);

        verify(categoryRepository).saveAll(categoriesCaptor.capture());
        List<String> expenseNames = List.of("Alimentación", "Transporte", "Vivienda", "Salud", "Entretenimiento", "Educación");
        long expenseCount = categoriesCaptor.getValue().stream()
                .filter(c -> expenseNames.contains(c.getNombre()))
                .count();
        assertEquals(6, expenseCount);
    }

    @Test
    @DisplayName("All categories should be associated with the provided user")
    void allShouldBelongToUser() {
        User user = new User();
        user.setId(5L);
        user.setEmail("owner@email.com");

        categoryInitService.crearCategoriasPorDefecto(user);

        verify(categoryRepository).saveAll(categoriesCaptor.capture());
        boolean allBelongToUser = categoriesCaptor.getValue().stream()
                .allMatch(c -> c.getUsuario().equals(user));
        assertTrue(allBelongToUser);
    }

    @Test
    @DisplayName("Should include the expected income categories")
    void shouldIncludeExpectedIncomeCategories() {
        User user = new User();
        user.setId(1L);

        categoryInitService.crearCategoriasPorDefecto(user);

        verify(categoryRepository).saveAll(categoriesCaptor.capture());
        List<String> names = categoriesCaptor.getValue().stream()
                .map(Category::getNombre)
                .toList();
        assertTrue(names.contains("Salario"));
        assertTrue(names.contains("Freelance"));
        assertTrue(names.contains("Inversiones"));
        assertTrue(names.contains("Otros ingresos"));
    }

    @Test
    @DisplayName("Should include the expected expense categories")
    void shouldIncludeExpectedExpenseCategories() {
        User user = new User();
        user.setId(1L);

        categoryInitService.crearCategoriasPorDefecto(user);

        verify(categoryRepository).saveAll(categoriesCaptor.capture());
        List<String> names = categoriesCaptor.getValue().stream()
                .map(Category::getNombre)
                .toList();
        assertTrue(names.contains("Alimentación"));
        assertTrue(names.contains("Transporte"));
        assertTrue(names.contains("Vivienda"));
        assertTrue(names.contains("Salud"));
        assertTrue(names.contains("Entretenimiento"));
        assertTrue(names.contains("Educación"));
    }
}
