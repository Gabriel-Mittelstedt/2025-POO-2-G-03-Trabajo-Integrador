package com.unam.integrador.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.unam.integrador.model.enums.MetodoPago;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO (Data Transfer Object) para representar un Recibo.
 * 
 * Esta clase NO es una entidad de JPA - representa un comprobante
 * generado dinámicamente desde los datos de Pago.
 * 
 * Elimina la redundancia de tener una tabla Recibo que duplica
 * información ya presente en Pago.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReciboDTO {
    
    /**
     * Número único del recibo.
     * Se genera a partir del ID del pago o secuencia custom.
     */
    private String numero;
    
    /**
     * Fecha de emisión del recibo (fecha del pago).
     */
    private LocalDate fecha;
    
    /**
     * Monto total del recibo.
     */
    private BigDecimal monto;
    
    /**
     * Método de pago utilizado.
     */
    private MetodoPago metodoPago;
    
    /**
     * Método de pago para mostrar en UI (incluye combinaciones).
     * Ejemplo: "EFECTIVO + SALDO A FAVOR"
     */
    private String metodoPagoDisplay;
    
    /**
     * Referencia o comprobante del pago (opcional).
     */
    private String referencia;
    
    /**
     * Descripción de las facturas asociadas al pago.
     * Ejemplo: "Factura 0001-00000123, Factura 0001-00000124"
     */
    private String facturasAsociadas;
    
    /**
     * Lista de IDs de facturas pagadas (para enlaces en la vista).
     */
    private List<Long> facturasIds;
    
    /**
     * Nombre del cliente que realizó el pago.
     */
    private String clienteNombre;
    
    /**
     * CUIT/DNI del cliente.
     */
    private String clienteCuitDni;
    
    /**
     * ID del cliente (para navegación).
     */
    private Long clienteId;
    
    /**
     * ID del pago asociado (para trazabilidad).
     */
    private Long pagoId;
    
    /**
     * Información adicional del recibo.
     * Ejemplo: "Saldo a favor aplicado: $500.00"
     */
    private String observaciones;
    
    /**
     * Desglose de pagos que componen este recibo.
     * Útil para pagos combinados con múltiples métodos de pago.
     */
    private List<DetallePagoDTO> desglosePagos;
    
    /**
     * DTO interno para representar cada línea del desglose de pagos.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetallePagoDTO {
        private MetodoPago metodoPago;
        private String numeroFactura;
        private Long facturaId;
        private BigDecimal monto;
    }
}
