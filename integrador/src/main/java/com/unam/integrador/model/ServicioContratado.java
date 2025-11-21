package com.unam.integrador.model;
import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;

/**
 * Representa la contratación de un servicio por parte de un cliente.
 * Registra el precio contratado y la fecha de alta, permitiendo históricos.
 */
@Data
@Entity
public class ServicioContratado {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long IDServicioContratado;
    
    @Column(nullable = false)
    private LocalDate fechaAlta;
    
    @Column(precision = 10, scale = 2) 
    private BigDecimal precioContratado; 
    
    @Column(nullable = false)
    private Boolean activo = true;
    
    /**
     * Fecha en que se desvinculó el servicio del cliente.
     * Null si el servicio sigue activo.
     */
    private LocalDate fechaBaja;
    
    // --- Relaciones ---
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private CuentaCliente cliente;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;
    
    // --- Métodos de negocio ---
    
    /**
     * Desvincula el servicio del cliente (baja lógica).
     * Marca el servicio como inactivo y registra la fecha de baja.
     * 
     * <p>Este servicio no se incluirá en futuras facturaciones masivas.</p>
     */
    public void desvincular() {
        this.activo = false;
        this.fechaBaja = LocalDate.now();
    }
}
