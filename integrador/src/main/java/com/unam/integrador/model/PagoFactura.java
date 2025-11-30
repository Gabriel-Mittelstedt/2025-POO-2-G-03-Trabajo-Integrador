package com.unam.integrador.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
public class PagoFactura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pago_id", nullable = false)
    private Pago pago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id", nullable = false)
    private Factura factura;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montoAplicado;

    private LocalDate fechaAplicacion;

    @Column(length = 500)
    private String referencia;

    public PagoFactura(Pago pago, Factura factura, BigDecimal montoAplicado, LocalDate fechaAplicacion, String referencia) {
        this.pago = pago;
        this.factura = factura;
        this.montoAplicado = montoAplicado;
        this.fechaAplicacion = fechaAplicacion;
        this.referencia = referencia;
    }
}
