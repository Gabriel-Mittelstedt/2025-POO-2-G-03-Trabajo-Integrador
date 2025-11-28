package com.unam.integrador.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO para recibir los datos del formulario de facturación masiva.
 */
@Data
public class FacturacionMasivaDTO {
    
    /**
     * Período de facturación (ej: "Noviembre 2024")
     */
    @NotBlank(message = "El período es obligatorio")
    @Size(min = 3, max = 50, message = "El período debe tener entre 3 y 50 caracteres")
    private String periodo;
    
    /**
     * Fecha de vencimiento para todas las facturas
     */
    @NotNull(message = "La fecha de vencimiento es obligatoria")
    @Future(message = "La fecha de vencimiento debe ser posterior a hoy")
    private LocalDate fechaVencimiento;
    
    /**
     * Usuario que ejecuta la facturación
     */
    @NotBlank(message = "El usuario es obligatorio")
    @Size(min = 2, max = 100, message = "El usuario debe tener entre 2 y 100 caracteres")
    private String usuario;
}
