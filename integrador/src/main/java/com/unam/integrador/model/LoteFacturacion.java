package com.unam.integrador.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que representa un lote de facturación masiva.
 * Agrupa todas las facturas generadas en una ejecución de facturación masiva
 * para un período determinado.
 */
@Data
@Entity
@NoArgsConstructor
public class LoteFacturacion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Período de facturación (ej: "Noviembre 2024", "Diciembre 2024")
     */
    @Column(nullable = false)
    private String periodo;
    
    /**
     * Fecha y hora en que se ejecutó la facturación masiva
     */
    @Column(nullable = false)
    private LocalDateTime fechaEjecucion;
    
    /**
     * Fecha de vencimiento común para todas las facturas del lote
     */
    @Column(nullable = false)
    private LocalDate fechaVencimiento;
    
    /**
     * Usuario que ejecutó la facturación masiva
     */
    @Column(nullable = false)
    private String usuarioEjecucion;
    
    /**
     * Cantidad de facturas generadas en el lote
     */
    @Column(nullable = false)
    private int cantidadFacturas;
    
    /**
     * Monto total de todas las facturas del lote
     */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal montoTotal;
    
    /**
     * Indica si el lote fue anulado
     */
    @Column(nullable = false)
    private boolean anulado = false;
    
    /**
     * Fecha de anulación del lote (si aplica)
     */
    private LocalDateTime fechaAnulacion;
    
    /**
     * Motivo de anulación del lote (si aplica)
     */
    @Column(length = 500)
    private String motivoAnulacion;
    
    /**
     * Usuario que anuló el lote (si aplica)
     */
    private String usuarioAnulacion;
    
    /**
     * Lista de facturas generadas en este lote
     */
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_facturacion_id")
    private List<Factura> facturas = new ArrayList<>();
    
    // --- CONSTRUCTOR ---
    
    /**
     * Constructor para crear un nuevo lote de facturación
     * @param periodo Período de facturación
     * @param fechaVencimiento Fecha de vencimiento de las facturas
     * @param usuarioEjecucion Usuario que ejecuta la facturación
     */
    public LoteFacturacion(String periodo, LocalDate fechaVencimiento, String usuarioEjecucion) {
        this.periodo = periodo;
        this.fechaVencimiento = fechaVencimiento;
        this.usuarioEjecucion = usuarioEjecucion;
        this.fechaEjecucion = LocalDateTime.now();
        this.cantidadFacturas = 0;
        this.montoTotal = BigDecimal.ZERO;
        this.anulado = false;
    }
    
    // --- MÉTODOS DE NEGOCIO ---
    
    /**
     * Agrega una factura al lote y actualiza los totales
     * @param factura Factura a agregar
     */
    public void agregarFactura(Factura factura) {
        this.facturas.add(factura);
        this.cantidadFacturas++;
        this.montoTotal = this.montoTotal.add(factura.getTotal());
    }
    
    /**
     * Verifica si el lote puede ser anulado.
     * Un lote solo puede anularse si no está ya anulado y
     * todas sus facturas pueden ser anuladas.
     * @return true si el lote puede ser anulado
     */
    public boolean puedeSerAnulado() {
        if (this.anulado) {
            return false;
        }
        
        // Verificar que todas las facturas puedan ser anuladas
        return this.facturas.stream().allMatch(Factura::puedeSerAnulada);
    }
    
    /**
     * Marca el lote como anulado.
     * Este método solo cambia el estado del lote, la anulación
     * de facturas individuales debe hacerse en el servicio.
     * @param motivo Motivo de la anulación
     * @param usuario Usuario que realiza la anulación
     * @throws IllegalStateException si el lote no puede ser anulado
     */
    public void anular(String motivo, String usuario) {
        if (!puedeSerAnulado()) {
            throw new IllegalStateException(
                "No se puede anular el lote. El lote ya está anulado o tiene facturas con pagos."
            );
        }
        
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IllegalArgumentException("El motivo de anulación es obligatorio");
        }
        
        if (usuario == null || usuario.trim().isEmpty()) {
            throw new IllegalArgumentException("El usuario de anulación es obligatorio");
        }
        
        this.anulado = true;
        this.fechaAnulacion = LocalDateTime.now();
        this.motivoAnulacion = motivo.trim();
        this.usuarioAnulacion = usuario.trim();
    }
    
    /**
     * Obtiene las facturas que pueden ser anuladas del lote
     * @return Lista de facturas anulables
     */
    public List<Factura> getFacturasAnulables() {
        return this.facturas.stream()
            .filter(Factura::puedeSerAnulada)
            .toList();
    }
    
    /**
     * Obtiene la cantidad de facturas que ya tienen pagos
     * y no pueden ser anuladas
     * @return Cantidad de facturas con pagos
     */
    public long getCantidadFacturasConPagos() {
        return this.facturas.stream()
            .filter(f -> !f.puedeSerAnulada())
            .count();
    }
}
