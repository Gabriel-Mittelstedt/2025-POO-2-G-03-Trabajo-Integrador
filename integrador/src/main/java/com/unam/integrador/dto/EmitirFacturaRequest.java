package com.unam.integrador.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la solicitud de emisión de factura individual.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmitirFacturaRequest {
    
    @NotNull(message = "El ID del cliente es obligatorio")
    private Long clienteId;
    
    @NotBlank(message = "El período es obligatorio")
    private String periodo;
    
    @NotNull(message = "La fecha de emisión es obligatoria")
    private LocalDate fechaEmision;
    
    @Min(value = 0, message = "El descuento no puede ser negativo")
    @Max(value = 100, message = "El descuento no puede ser mayor a 100%")
    private Double porcentajeDescuento;
    
    private String motivoDescuento;
    
    @NotEmpty(message = "Debe incluir al menos un item")
    @Valid
    private List<ItemFacturaRequest> items;
}
