package com.unam.integrador.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.unam.integrador.model.enums.EstadoFactura;
import com.unam.integrador.model.enums.TipoFactura;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la respuesta con información de una factura.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FacturaResponse {
    
    private Long idFactura;
    private int serie;
    private int nroFactura;
    private String numeroCompleto;
    
    // Cliente
    private Long clienteId;
    private String clienteNombre;
    private String clienteCuit;
    
    // Fechas y período
    private LocalDate fechaEmision;
    private LocalDate fechaVencimiento;
    private String periodo;
    
    // Tipo y estado
    private TipoFactura tipo;
    private EstadoFactura estado;
    
    // Montos
    private BigDecimal subtotal;
    private double descuento;
    private String motivoDescuento;
    private BigDecimal totalIva;
    private BigDecimal total;
    private BigDecimal saldoPendiente;
    
    // Detalle
    private List<ItemFacturaDTO> items;
    
    /**
     * DTO para los items de la factura.
     */
    @Data
    public static class ItemFacturaDTO {
        private String descripcion;
        private BigDecimal precioUnitario;
        private int cantidad;
        private String alicuotaIVA;
        private BigDecimal subtotal;
        private BigDecimal montoIva;
        private BigDecimal total;
    }
    
    /**
     * Genera el número completo de factura con formato SERIE-NUMERO.
     */
    public String generarNumeroCompleto() {
        this.numeroCompleto = String.format("%03d-%08d", serie, nroFactura);
        return this.numeroCompleto;
    }
}
