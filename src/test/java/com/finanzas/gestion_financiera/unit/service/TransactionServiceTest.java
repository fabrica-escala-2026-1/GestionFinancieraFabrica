package com.finanzas.gestion_financiera.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
import org.springframework.security.test.context.support.WithMockUser;

import com.finanzas.gestion_financiera.dto.GastoPorCategoriaResponse;
import com.finanzas.gestion_financiera.dto.ResumenGastosMensualesResponse;
import com.finanzas.gestion_financiera.dto.TransactionRequest;
import com.finanzas.gestion_financiera.dto.TransactionResponse;
import com.finanzas.gestion_financiera.entity.Category;
import com.finanzas.gestion_financiera.entity.Transaction;
import com.finanzas.gestion_financiera.entity.User;
import com.finanzas.gestion_financiera.repository.CategoryRepository;
import com.finanzas.gestion_financiera.repository.TransactionRepository;
import com.finanzas.gestion_financiera.repository.UserRepository;
import com.finanzas.gestion_financiera.service.TransactionService;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService - Unit Tests")
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User testUser;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@email.com");

        testCategory = new Category();
        testCategory.setId(10L);
        testCategory.setNombre("Salario");
        testCategory.setTipo(Category.TipoCategoria.INGRESO);
        testCategory.setUsuario(testUser);

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
        @DisplayName("Should create an INGRESO transaction correctly")
        void shouldCreateIncomeTransaction() {
            TransactionRequest request = new TransactionRequest();
            request.setTipo("INGRESO");
            request.setMonto(new BigDecimal("5000.00"));
            request.setCategoriaId(10L);

            when(categoryRepository.findById(10L)).thenReturn(Optional.of(testCategory));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
                Transaction t = invocation.getArgument(0);
                t.setId(1L);
                t.setFecha(LocalDate.now());
                return t;
            });

            TransactionResponse response = transactionService.crear(request);

            assertNotNull(response);
            assertEquals(1L, response.getId());
            assertEquals("INGRESO", response.getTipo());
            assertEquals(new BigDecimal("5000.00"), response.getMonto());
            assertEquals("Salario", response.getCategoria());
            verify(transactionRepository).save(any(Transaction.class));
        }

        @Test
        @DisplayName("Should create a GASTO transaction correctly")
        void shouldCreateExpenseTransaction() {
            Category expenseCategory = new Category();
            expenseCategory.setId(20L);
            expenseCategory.setNombre("Alimentación");
            expenseCategory.setTipo(Category.TipoCategoria.GASTO);

            TransactionRequest request = new TransactionRequest();
            request.setTipo("GASTO");
            request.setMonto(new BigDecimal("150.50"));
            request.setCategoriaId(20L);

            when(categoryRepository.findById(20L)).thenReturn(Optional.of(expenseCategory));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> {
                Transaction t = i.getArgument(0);
                t.setId(2L);
                t.setFecha(LocalDate.now());
                return t;
            });

            TransactionResponse response = transactionService.crear(request);

            assertEquals("GASTO", response.getTipo());
            assertEquals(new BigDecimal("150.50"), response.getMonto());
            assertEquals("Alimentación", response.getCategoria());
        }

        @Test
        @DisplayName("Should throw exception when the category does not exist")
        void shouldThrowExceptionWhenCategoryDoesNotExist() {
            TransactionRequest request = new TransactionRequest();
            request.setTipo("INGRESO");
            request.setMonto(new BigDecimal("100"));
            request.setCategoriaId(999L);

            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> transactionService.crear(request));
            assertEquals("Categoría no válida", exception.getMessage());
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when the user does not exist")
        void shouldThrowExceptionWhenUserDoesNotExist() {
            TransactionRequest request = new TransactionRequest();
            request.setTipo("INGRESO");
            request.setMonto(new BigDecimal("100"));
            request.setCategoriaId(10L);

            when(userRepository.findByEmail("test@email.com")).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class,
                    () -> transactionService.crear(request));
        }
    }

    @Nested
    @DisplayName("listar()")
    class List_ {

        @Test
        @DisplayName("Should list transactions of the authenticated user")
        void shouldListUserTransactions() {
            Transaction t1 = new Transaction();
            t1.setId(1L);
            t1.setTipo("INGRESO");
            t1.setMonto(new BigDecimal("3000"));
            t1.setFecha(LocalDate.of(2026, 4, 1));
            t1.setUsuario(testUser);
            t1.setCategoria(testCategory);

            Transaction t2 = new Transaction();
            t2.setId(2L);
            t2.setTipo("GASTO");
            t2.setMonto(new BigDecimal("500"));
            t2.setFecha(LocalDate.of(2026, 4, 2));
            t2.setUsuario(testUser);
            t2.setCategoria(testCategory);

            when(transactionRepository.findByUsuarioId(1L)).thenReturn(List.of(t1, t2));

            List<TransactionResponse> result = transactionService.listar();

            assertEquals(2, result.size());
            assertEquals("INGRESO", result.get(0).getTipo());
            assertEquals(new BigDecimal("3000"), result.get(0).getMonto());
            assertEquals("GASTO", result.get(1).getTipo());
            assertEquals(new BigDecimal("500"), result.get(1).getMonto());
        }

        @Test
        @DisplayName("Should return an empty list when there are no transactions")
        void shouldReturnEmptyList() {
            when(transactionRepository.findByUsuarioId(1L)).thenReturn(List.of());

            List<TransactionResponse> result = transactionService.listar();

            assertTrue(result.isEmpty());
        }

        @Test
        @WithMockUser(username = "test@email.com")
        void obtenerResumenGastosPorCategoriaConDatosExito() {
            // Arrange
            Integer anio = 2026;
            Integer mes = 5;
            
            GastoPorCategoriaResponse gastoMock = new GastoPorCategoriaResponse("Comida", new BigDecimal("150.00"));
            List<GastoPorCategoriaResponse> listaMock = List.of(gastoMock);

            doReturn(Optional.of(testUser)).when(userRepository).findByEmail("test@email.com");
            
            doReturn(listaMock).when(transactionRepository)
                .obtenerGastosPorCategoriaDelMes(any(), any(), any());

            // Act  
            ResumenGastosMensualesResponse respuesta = transactionService.obtenerResumenGastosPorCategoria(anio, mes);

            // Assert
            assertNotNull(respuesta);
            assertNull(respuesta.getMensaje());
            assertFalse(respuesta.getCategorias().isEmpty());
        }

        @Test
        @DisplayName("Debe lanzar excepción si el tipo no es INGRESO ni GASTO")
        void crearTransaccionTipoInvalidoLanzaException() {
            TransactionRequest request = new TransactionRequest();
            request.setTipo("OTRO"); // Rompe el primer if
            request.setMonto(new BigDecimal("100.00"));
            
            assertThrows(IllegalArgumentException.class, () -> {
                transactionService.crear(request);
            });
        }

        @Test
        @DisplayName("Debe lanzar excepción si el monto es menor o igual a cero")
        void crearTransaccionMontoInvalidoLanzaException() {
            TransactionRequest request = new TransactionRequest();
            request.setTipo("GASTO");
            request.setMonto(BigDecimal.ZERO); // Rompe el segundo if (<= 0)
            
            assertThrows(IllegalArgumentException.class, () -> {
                transactionService.crear(request);
            });
        }
    }
}
