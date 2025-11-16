package com.unam.integrador.dto;

import java.math.BigDecimal;

import com.unam.integrador.model.enums.TipoAlicuotaIVA;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para representar un item de factura en el request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemFacturaRequest {
    
    @NotBlank(message = "La descripción es obligatoria")
    private String descripcion;
    
    @NotNull(message = "El precio unitario es obligatorio")
    @Min(value = 0, message = "El precio no puede ser negativo")
    private BigDecimal precioUnitario;
    
    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer cantidad;
    
    @NotNull(message = "La alícuota de IVA es obligatoria")
    private TipoAlicuotaIVA alicuotaIVA;
}
