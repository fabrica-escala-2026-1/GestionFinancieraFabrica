package com.finanzas.gestion_financiera.service;

import com.finanzas.gestion_financiera.dto.BalanceResponse;
import com.finanzas.gestion_financiera.entity.User;
import com.finanzas.gestion_financiera.repository.TransactionRepository;
import com.finanzas.gestion_financiera.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class BalanceService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public BalanceResponse obtenerBalanceMesActual() {
        YearMonth mesActual = YearMonth.now();

        LocalDate fechaInicio = mesActual.atDay(1);
        LocalDate fechaFin = mesActual.atEndOfMonth();

        return obtenerBalancePorPeriodo(fechaInicio, fechaFin);
    }

    public BalanceResponse obtenerBalancePorPeriodo(LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio == null || fechaFin == null) {
            throw new IllegalArgumentException("Las fechas de inicio y fin son obligatorias");
        }

        if (fechaInicio.isAfter(fechaFin)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha final");
        }

        User user = getAuthenticatedUser();

        BigDecimal ingresos = transactionRepository.sumByTipoAndUsuarioAndPeriod(
                user.getId(),
                "INGRESO",
                fechaInicio,
                fechaFin
        );

        BigDecimal gastos = transactionRepository.sumByTipoAndUsuarioAndPeriod(
                user.getId(),
                "GASTO",
                fechaInicio,
                fechaFin
        );

        BigDecimal balanceNeto = ingresos.subtract(gastos);
        boolean balanceNegativo = balanceNeto.compareTo(BigDecimal.ZERO) < 0;

        String mensaje = balanceNegativo
                ? "Tu balance es negativo. Revisa tus movimientos para ajustar el descuadre"
                : "Tu balance financiero es positivo";

        return new BalanceResponse(
                fechaInicio,
                fechaFin,
                ingresos,
                gastos,
                balanceNeto,
                balanceNegativo,
                mensaje
        );
    }

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }
}