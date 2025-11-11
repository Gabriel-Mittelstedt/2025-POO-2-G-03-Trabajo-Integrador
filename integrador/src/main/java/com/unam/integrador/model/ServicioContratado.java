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
    
    // --- Relaciones ---
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private CuentaCliente cliente;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;
}
