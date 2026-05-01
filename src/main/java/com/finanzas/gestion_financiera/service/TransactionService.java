package com.finanzas.gestion_financiera.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public TransactionResponse crear(TransactionRequest request) {

        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!request.getTipo().equals("INGRESO") && !request.getTipo().equals("GASTO")) {
            throw new RuntimeException("El tipo debe ser INGRESO o GASTO");
        }

        if (request.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Debes ingresar un monto válido");
        }

        Category category = categoryRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new RuntimeException("Categoría no válida"));

        Transaction transaction = new Transaction();
        transaction.setTipo(request.getTipo());
        transaction.setMonto(request.getMonto());
        transaction.setFecha(LocalDate.now()); // se asigna automáticamente
        transaction.setUsuario(user);
        transaction.setCategoria(category);

        transactionRepository.save(transaction);

        return new TransactionResponse(
                transaction.getId(),
                transaction.getTipo(),
                transaction.getMonto(),
                transaction.getFecha(),
                category.getNombre()
        );
    }

    public List<TransactionResponse> listar() {
        // Obtener el usuario autenticado desde el token
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return transactionRepository.findByUsuarioId(user.getId())
                .stream()
                .map(t -> new TransactionResponse(
                        t.getId(),
                        t.getTipo(),
                        t.getMonto(),
                        t.getFecha(),
                        t.getCategoria().getNombre()
                ))
                .collect(Collectors.toList());
    }

    public ResumenGastosMensualesResponse obtenerResumenGastosPorCategoria(Integer anio, Integer mes) {

        if (mes < 1 || mes > 12) {
            throw new RuntimeException("El mes debe estar entre 1 y 12");
        }

        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        YearMonth yearMonth = YearMonth.of(anio, mes);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<GastoPorCategoriaResponse> categorias =
                transactionRepository.obtenerGastosPorCategoriaDelMes(
                        user.getId(),
                        startDate,
                        endDate
                );

        if (categorias.isEmpty()) {
            return new ResumenGastosMensualesResponse(
                    anio,
                    mes,
                    "No tienes transacciones registradas para este período",
                    Collections.emptyList()
            );
        }

        return new ResumenGastosMensualesResponse(
                anio,
                mes,
                null,
                categorias
        );
    }
}