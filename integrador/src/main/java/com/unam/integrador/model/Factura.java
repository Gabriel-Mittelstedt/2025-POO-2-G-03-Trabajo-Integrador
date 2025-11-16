package com.unam.integrador.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.unam.integrador.model.enums.EstadoFactura;
import com.unam.integrador.model.enums.TipoFactura;

import jakarta.persistence.*;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
public class Factura {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idFactura;

    @Column(nullable = false)
    private int serie;

    @Column(nullable = false)
    private int nroFactura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private CuentaCliente cliente;

    // --- Atributos de la Factura ---
    private LocalDate fechaEmision;
    private LocalDate fechaVencimiento;
    private String periodo;

    @Enumerated(EnumType.STRING)
    private TipoFactura tipo;
    
    @Enumerated(EnumType.STRING)
    private EstadoFactura estado;

    // --- Campos Calculados y Opcionales ---
    // Se inicializan en 0 o null y se calculan con un método.
    private BigDecimal subtotal;
    private double descuento;
    private String motivoDescuento;
    private BigDecimal totalIva;
    private BigDecimal saldoPendiente;
    private BigDecimal total;

    //--Relaciones--
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id") 
    private List<ItemFactura> detalleFactura = new ArrayList<>();

    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<NotaCredito> notasCredito = new ArrayList<>();

    //CONSTRUCTOR
    public Factura(int serie, int nroFactura, CuentaCliente cliente, LocalDate fechaEmision, 
                   LocalDate fechaVencimiento, String periodo, TipoFactura tipo) {
        
        this.serie = serie;
        this.nroFactura = nroFactura;
        this.cliente = cliente;
        this.fechaEmision = fechaEmision;
        this.fechaVencimiento = fechaVencimiento;
        this.periodo = periodo;
        this.tipo = tipo;

        // --- Valores por defecto al crear una factura ---
        this.estado = EstadoFactura.PENDIENTE;
        this.subtotal = BigDecimal.ZERO;
        this.descuento = 0.0;
        this.totalIva = BigDecimal.ZERO;
        this.saldoPendiente = BigDecimal.ZERO;
        this.total = BigDecimal.ZERO;
        this.motivoDescuento = null;
    }
}
