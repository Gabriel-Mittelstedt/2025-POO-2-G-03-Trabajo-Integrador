package com.unam.integrador.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.unam.integrador.model.enums.EstadoFactura;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Entidad que representa un lote de facturación masiva.
 * Un lote agrupa todas las facturas generadas en una ejecución de facturación masiva.
 * 
 * Cumple con HU-07, HU-08 y HU-09 del ERP.
 */
@Data
@Entity
@NoArgsConstructor
public class LoteFacturacion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Período de facturación en formato texto (ej: "Noviembre 2025").
     */
    @Column(nullable = false)
    private String periodo;
    
    /**
     * Período de facturación como LocalDate (primer día del mes).
     */
    @Column(nullable = false)
    private LocalDate periodoFecha;
    
    /**
     * Fecha y hora en que se ejecutó la facturación masiva.
     */
    @Column(nullable = false)
    private LocalDateTime fechaEjecucion;
    
    /**
     * Fecha de vencimiento aplicada a todas las facturas del lote.
     */
    @Column(nullable = false)
    private LocalDate fechaVencimiento;
    
    /**
     * Cantidad de facturas generadas en el lote.
     */
    @Column(nullable = false)
    private int cantidadFacturas;
    
    /**
     * Monto total del lote (suma de todas las facturas).
     */
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal montoTotal;
    
    /**
     * Indica si el lote fue anulado.
     */
    @Column(nullable = false)
    private boolean anulado = false;
    
    /**
     * Fecha de anulación del lote (si aplica).
     */
    private LocalDateTime fechaAnulacion;
    
    /**
     * Motivo de anulación del lote (si aplica).
     */
    private String motivoAnulacion;
    

    
    /**
     * Lista de facturas generadas en este lote.
     */
    @OneToMany(mappedBy = "loteFacturacion", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Factura> facturas = new ArrayList<>();
    
    /**
     * Constructor para crear un nuevo lote de facturación.
     * 
     * @param periodo Período en formato texto (ej: "Noviembre 2025")
     * @param periodoFecha Fecha del período (primer día del mes)
     * @param fechaVencimiento Fecha de vencimiento para las facturas
     */
    public LoteFacturacion(String periodo, LocalDate periodoFecha, 
                           LocalDate fechaVencimiento) {
        this.periodo = periodo;
        this.periodoFecha = periodoFecha;
        this.fechaEjecucion = LocalDateTime.now();
        this.fechaVencimiento = fechaVencimiento;
        this.cantidadFacturas = 0;
        this.montoTotal = BigDecimal.ZERO;
        this.anulado = false;
    }
    
    // --- MÉTODOS DE NEGOCIO (Modelo RICO) ---
    
    /**
     * Agrega una factura al lote y actualiza los totales.
     * 
     * @param factura La factura a agregar
     */
    public void agregarFactura(Factura factura) {
        if (factura == null) {
            throw new IllegalArgumentException("La factura no puede ser nula");
        }
        
        factura.setLoteFacturacion(this);
        this.facturas.add(factura);
        this.cantidadFacturas++;
        this.montoTotal = this.montoTotal.add(factura.getTotal());
    }
    
    /**
     * Verifica si el lote puede ser anulado.
     * Solo se puede anular si ninguna factura tiene pagos registrados.
     * 
     * @return true si el lote puede ser anulado
     */
    public boolean puedeSerAnulado() {
        if (this.anulado) {
            return false;
        }
        
        // Verificar que ninguna factura tenga pagos
        for (Factura factura : this.facturas) {
            // Una factura no puede ser anulada si tiene pagos parciales o totales
            if (factura.getEstado() == EstadoFactura.PAGADA_PARCIALMENTE ||
                factura.getEstado() == EstadoFactura.PAGADA_TOTALMENTE) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Anula el lote de facturación.
     * Cambia el estado del lote a anulado y registra la información de anulación.
     * 
     * @param motivo Motivo de la anulación
     * @throws IllegalStateException si el lote no puede ser anulado
     */
    public void anular(String motivo) {
        if (!puedeSerAnulado()) {
            throw new IllegalStateException(
                "No se puede anular el lote. Algunas facturas ya tienen pagos registrados."
            );
        }
        
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IllegalArgumentException("El motivo de anulación es obligatorio");
        }
        
        this.anulado = true;
        this.fechaAnulacion = LocalDateTime.now();
        this.motivoAnulacion = motivo.trim();
    }
    
    /**
     * Obtiene las facturas que no están anuladas.
     * 
     * @return Lista de facturas activas del lote
     */
    public List<Factura> getFacturasActivas() {
        return this.facturas.stream()
            .filter(f -> f.getEstado() != EstadoFactura.ANULADA)
            .toList();
    }
    
    /**
     * Obtiene la cantidad de facturas que fueron anuladas.
     * 
     * @return Número de facturas anuladas
     */
    public int getCantidadFacturasAnuladas() {
        return (int) this.facturas.stream()
            .filter(f -> f.getEstado() == EstadoFactura.ANULADA)
            .count();
    }
    
    /**
     * Calcula el monto total de las facturas activas (no anuladas).
     * 
     * @return Monto total activo
     */
    public BigDecimal getMontoTotalActivo() {
        return this.facturas.stream()
            .filter(f -> f.getEstado() != EstadoFactura.ANULADA)
            .map(Factura::getTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
