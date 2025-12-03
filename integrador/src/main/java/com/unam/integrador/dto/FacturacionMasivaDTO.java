package com.unam.integrador.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.AssertTrue;
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
    
    /**
     * Valida que la fecha de vencimiento sea posterior a la fecha actual (fecha de emisión).
     * Esta validación se ejecuta como parte de la validación del DTO.
     * 
     * @return true si la fecha de vencimiento es válida (posterior a hoy), false en caso contrario
     */
    @AssertTrue(message = "La fecha de vencimiento debe ser posterior a la fecha de emisión (hoy)")
    public boolean isFechaVencimientoValida() {
        if (fechaVencimiento == null) {
            return true; // La validación de @NotNull se encarga de este caso
        }
        return fechaVencimiento.isAfter(LocalDate.now());
    }
}
