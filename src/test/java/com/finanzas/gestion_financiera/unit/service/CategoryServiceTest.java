package com.finanzas.gestion_financiera.unit.service;

import com.finanzas.gestion_financiera.dto.CategoryRequest;
import com.finanzas.gestion_financiera.dto.CategoryResponse;
import com.finanzas.gestion_financiera.entity.Category;
import com.finanzas.gestion_financiera.entity.User;
import com.finanzas.gestion_financiera.repository.CategoryRepository;
import com.finanzas.gestion_financiera.repository.UserRepository;
import com.finanzas.gestion_financiera.service.CategoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService - Unit Tests")
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CategoryService categoryService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@email.com");
        testUser.setPrimer_nombre("Test");
        testUser.setApellido("User");

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("test@email.com")
                .password("{noop}")
                .authorities(List.of())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );

        lenient().when(userRepository.findByEmail("test@email.com")).thenReturn(Optional.of(testUser));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("crear()")
    class Create {

        @Test
        @DisplayName("Should create a category correctly")
        void shouldCreateIncomeCategory() {
            CategoryRequest request = new CategoryRequest();
            request.setNombre("Bonificación");

            when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
                Category c = invocation.getArgument(0);
                c.setId(1L);
                return c;
            });

            CategoryResponse response = categoryService.crear(request);

            assertNotNull(response);
            assertEquals("Bonificación", response.getNombre());
            verify(categoryRepository).save(any(Category.class));
        }

        @Test
        @DisplayName("Should create another category correctly")
        void shouldCreateExpenseCategory() {
            CategoryRequest request = new CategoryRequest();
            request.setNombre("Restaurantes");

            when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
                Category c = invocation.getArgument(0);
                c.setId(2L);
                return c;
            });

            CategoryResponse response = categoryService.crear(request);

            assertEquals("Restaurantes", response.getNombre());
        }
    }

    @Nested
    @DisplayName("listar()")
    class List_ {

        @Test
        @DisplayName("Should list categories of the authenticated user")
        void shouldListUserCategories() {
            Category cat1 = new Category();
            cat1.setId(1L);
            cat1.setNombre("Salario");
            cat1.setUsuario(testUser);

            Category cat2 = new Category();
            cat2.setId(2L);
            cat2.setNombre("Alimentación");
            cat2.setUsuario(testUser);

            when(categoryRepository.findByUsuarioId(1L)).thenReturn(List.of(cat1, cat2));

            List<CategoryResponse> result = categoryService.listar();

            assertEquals(2, result.size());
            assertEquals("Salario", result.get(0).getNombre());
            assertEquals("Alimentación", result.get(1).getNombre());
        }

        @Test
        @DisplayName("Should return an empty list when there are no categories")
        void shouldReturnEmptyList() {
            when(categoryRepository.findByUsuarioId(1L)).thenReturn(List.of());

            List<CategoryResponse> result = categoryService.listar();

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("obtener()")
    class Get {

        @Test
        @DisplayName("Should fetch the category by ID for the authenticated user")
        void shouldGetCategoryById() {
            Category category = new Category();
            category.setId(5L);
            category.setNombre("Transporte");
            category.setUsuario(testUser);

            when(categoryRepository.findByIdAndUsuarioId(5L, 1L)).thenReturn(Optional.of(category));

            CategoryResponse response = categoryService.obtener(5L);

            assertEquals(5L, response.getId());
            assertEquals("Transporte", response.getNombre());
        }

        @Test
        @DisplayName("Should throw exception when the category does not exist")
        void shouldThrowExceptionWhenNotFound() {
            when(categoryRepository.findByIdAndUsuarioId(99L, 1L)).thenReturn(Optional.empty());

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> categoryService.obtener(99L));
            assertEquals("Categoría no encontrada", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("actualizar()")
    class Update {

        @Test
        @DisplayName("Should update the name of an existing category")
        void shouldUpdateCategory() {
            Category existing = new Category();
            existing.setId(3L);
            existing.setNombre("Vieja");
            existing.setUsuario(testUser);

            CategoryRequest request = new CategoryRequest();
            request.setNombre("Nueva");

            when(categoryRepository.findByIdAndUsuarioId(3L, 1L)).thenReturn(Optional.of(existing));
            when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

            CategoryResponse response = categoryService.actualizar(3L, request);

            assertEquals("Nueva", response.getNombre());
            verify(categoryRepository).save(existing);
        }

        @Test
        @DisplayName("Should throw exception when updating a nonexistent category")
        void shouldThrowExceptionWhenUpdatingNonexistent() {
            CategoryRequest request = new CategoryRequest();
            request.setNombre("Test");

            when(categoryRepository.findByIdAndUsuarioId(99L, 1L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class,
                    () -> categoryService.actualizar(99L, request));
        }
    }

    @Nested
    @DisplayName("eliminar()")
    class Delete {

        @Test
        @DisplayName("Should delete an existing category for the user")
        void shouldDeleteCategory() {
            Category category = new Category();
            category.setId(4L);
            category.setNombre("Temporal");
            category.setUsuario(testUser);

            when(categoryRepository.findByIdAndUsuarioId(4L, 1L)).thenReturn(Optional.of(category));

            categoryService.eliminar(4L);

            verify(categoryRepository).delete(category);
        }

        @Test
        @DisplayName("Should throw exception when deleting a nonexistent category")
        void shouldThrowExceptionWhenDeletingNonexistent() {
            when(categoryRepository.findByIdAndUsuarioId(99L, 1L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class,
                    () -> categoryService.eliminar(99L));
        }
    }
}
