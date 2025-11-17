package com.unam.integrador.model.enums;

/**
 * Enum que representa los posibles estados de una factura.
 */
public enum EstadoFactura {
    
    /**
     * Factura emitida sin pagos registrados.
     */
    PENDIENTE("Pendiente"),
    
    /**
     * Factura con pagos parciales, saldo pendiente mayor a cero.
     */
    PAGADA_PARCIALMENTE("Pagada Parcialmente"),
    
    /**
     * Factura totalmente pagada, saldo pendiente en cero.
     */
    PAGADA_TOTALMENTE("Pagada Totalmente"),
    
    /**
     * Factura vencida sin pago completo.
     */
    VENCIDA("Vencida"),
    
    /**
     * Factura anulada por nota de crédito o corrección.
     */
    ANULADA("Anulada");
    
    /**
     * Descripción legible del estado para mostrar en la interfaz de usuario.
     */
    private final String descripcion;
    
    EstadoFactura(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public String getDescripcion() {
        return this.descripcion;
    }
}
