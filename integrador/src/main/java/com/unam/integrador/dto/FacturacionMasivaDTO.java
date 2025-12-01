package com.unam.integrador.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO para el formulario de facturación masiva.
 * Contiene los datos necesarios para ejecutar una facturación masiva.
 */
@Data
public class FacturacionMasivaDTO {
    
    /**
     * Período de facturación en formato texto (ej: "Noviembre 2025").
     */
    @NotBlank(message = "El período es obligatorio")
    private String periodo;
    
    /**
     * Fecha de vencimiento para todas las facturas del lote.
     */
    @NotNull(message = "La fecha de vencimiento es obligatoria")
    private LocalDate fechaVencimiento;
}
