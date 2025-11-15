package com.unam.integrador.model;

import java.math.BigDecimal;

import com.unam.integrador.model.enums.TipoAlicuotaIVA;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
public class ItemFactura {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- Datos del servicio al momento de facturar ---
    @Column(nullable = false)
    private String descripcion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false)
    private int cantidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoAlicuotaIVA alicuotaIVA;

    // --- Campos calculados ---
    @Column(precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(precision = 10, scale = 2)
    private BigDecimal montoIva;

    @Column(precision = 10, scale = 2)
    private BigDecimal total;

    // --- Relaciones ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id")
    private Factura factura;

    // --- Constructor ---
    public ItemFactura(String descripcion, BigDecimal precioUnitario, int cantidad, 
                       TipoAlicuotaIVA alicuotaIVA) {
        this.descripcion = descripcion;
        this.precioUnitario = precioUnitario;
        this.cantidad = cantidad;
        this.alicuotaIVA = alicuotaIVA;
        
        // Inicializar valores calculados
        this.subtotal = BigDecimal.ZERO;
        this.montoIva = BigDecimal.ZERO;
        this.total = BigDecimal.ZERO;
    }
}
