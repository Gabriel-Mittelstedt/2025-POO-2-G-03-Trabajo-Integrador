package com.unam.integrador.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.unam.integrador.model.enums.TipoFactura;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representa una nota de crédito emitida para anular una factura.
 * 
 * Las notas de crédito se generan automáticamente cuando se anula una factura,
 * reflejando el monto total de la misma y manteniendo la trazabilidad fiscal.
 * 
 * Características:
 * - Tiene su propia numeración secuencial por serie
 * - Mantiene el mismo tipo de factura que la factura anulada
 * - Registra el motivo de la anulación
 * - Es inmutable una vez creada
 * 
 * @author Sistema ERP Facturación
 * @version 1.0
 */
@Data
@Entity
@NoArgsConstructor
public class NotaCredito {
    
    /** Identificador único de la nota de crédito (clave primaria). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Serie de la nota de crédito (coincide con la serie de la factura). */
    @Column(nullable = false)
    private int serie;

    /** Número secuencial de la nota de crédito dentro de su serie. */
    @Column(nullable = false)
    private int nroNotaCredito;

    /** Fecha en que se emite la nota de crédito. */
    @Column(nullable = false)
    private LocalDate fechaEmision;

    /** Monto de la nota de crédito (normalmente igual al total de la factura anulada). */
    @Column(nullable = false)
    private BigDecimal monto;

    /** Motivo o razón de la emisión de la nota de crédito. */
    @Column(nullable = false)
    private String motivo;

    /** Tipo de nota de crédito (A, B o C, según la factura). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoFactura tipo;

    /** Factura que se anula con esta nota de crédito. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id", nullable = false) 
    private Factura factura;

    /**
     * Constructor para crear una nota de crédito.
     * 
     * @param serie Serie de la nota de crédito (debe coincidir con la factura)
     * @param nroNotaCredito Número secuencial de la nota de crédito
     * @param fechaEmision Fecha de emisión de la nota de crédito
     * @param monto Importe de la nota de crédito
     * @param motivo Justificación de la emisión
     * @param tipo Tipo de nota de crédito (A, B o C)
     * @param factura Factura asociada que se anula
     */
    public NotaCredito(int serie, int nroNotaCredito, LocalDate fechaEmision, 
                       BigDecimal monto, String motivo, TipoFactura tipo, 
                       Factura factura) {
        this.serie = serie;
        this.nroNotaCredito = nroNotaCredito;
        this.fechaEmision = fechaEmision;
        this.monto = monto;
        this.motivo = motivo;
        this.tipo = tipo;
        this.factura = factura;
    }

}
