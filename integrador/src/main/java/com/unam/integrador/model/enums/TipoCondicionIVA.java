package com.unam.integrador.model.enums;

/**
 * Enumeración que representa las condiciones fiscales de un cliente frente al IVA.
 * 
 * <p>Define la categoría tributaria del cliente según la normativa de AFIP Argentina,
 * determinando el tipo de factura a emitir y el tratamiento del IVA.</p>
 */
public enum TipoCondicionIVA {
    
    /**
     * Responsable Inscripto en IVA.
     * <p>Empresas o profesionales inscriptos en IVA que pueden emitir y recibir
     * Facturas A (entre inscriptos) o Facturas B (a consumidores finales).
     * Discriminan IVA en sus facturas.</p>
     */
    RESPONSABLE_INSCRIPTO("Responsable Inscripto"),
    
    /**
     * Monotributista.
     * <p>Pequeños contribuyentes bajo el Régimen Simplificado.
     * Emiten Facturas C que no discriminan IVA.
     * El impuesto está incluido en la cuota mensual del monotributo.</p>
     */
    MONOTRIBUTISTA("Monotributista"),
    
    /**
     * Exento de IVA.
     * <p>Contribuyentes cuya actividad está exenta del pago de IVA
     * por normativa específica (ej: educación, salud).
     * No discriminan IVA en sus operaciones.</p>
     */
    EXENTO("Exento"),
    
    /**
     * Consumidor Final.
     * <p>Personas físicas que no realizan actividad económica.
     * Reciben Facturas B donde el IVA está incluido en el precio.
     * No pueden computar crédito fiscal.</p>
     */
    CONSUMIDOR_FINAL("Consumidor Final");
    
    private final String descripcion;
    
    TipoCondicionIVA(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
}
