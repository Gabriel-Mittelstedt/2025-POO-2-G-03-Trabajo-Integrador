package com.unam.integrador.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.unam.integrador.model.enums.TipoFactura;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
public class NotaCredito {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int serie;

    @Column(nullable = false)
    private int nroNotaCredito;

    @Column(nullable = false)
    private LocalDate fechaEmision;

    @Column(nullable = false)
    private BigDecimal monto;

    @Column(nullable = false)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoFactura tipo;

    //Relacion con factura
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id", nullable = false) 
    private Factura factura;

    //Constructor
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
