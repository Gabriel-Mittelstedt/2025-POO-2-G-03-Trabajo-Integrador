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

    // --- MÉTODOS DE NEGOCIO (Modelo RICO) ---

    /**
     * Calcula el subtotal del item (precio × cantidad).
     * @return Subtotal sin IVA
     */
    public BigDecimal calcularSubtotal() {
        this.subtotal = precioUnitario.multiply(new BigDecimal(cantidad));
        return this.subtotal;
    }

    /**
     * Calcula el monto de IVA según la alícuota correspondiente.
     * @return Monto del IVA a aplicar
     */
    public BigDecimal calcularMontoIva() {
        BigDecimal alicuota = obtenerValorAlicuota();
        this.montoIva = this.subtotal.multiply(alicuota);
        return this.montoIva;
    }

    /**
     * Calcula el total del item (subtotal + IVA).
     * @return Total del item
     */
    public BigDecimal calcularTotal() {
        this.total = this.subtotal.add(this.montoIva);
        return this.total;
    }

    /**
     * Ejecuta todos los cálculos en el orden correcto.
     * Este método encapsula la lógica de cálculo completa del item.
     */
    public void calcular() {
        calcularSubtotal();
        calcularMontoIva();
        calcularTotal();
    }

    /**
     * Constructor para items con facturación proporcional.
     * Calcula automáticamente el precio proporcional basado en los días efectivos.
     * 
     * @param descripcionBase Descripción base del servicio
     * @param precioMensual Precio mensual del servicio
     * @param cantidad Cantidad del item (normalmente 1)
     * @param alicuotaIVA Alícuota de IVA a aplicar
     * @param periodo Período de facturación con días efectivos
     */
    public static ItemFactura crearProporcional(
            String descripcionBase, 
            BigDecimal precioMensual, 
            int cantidad,
            TipoAlicuotaIVA alicuotaIVA,
            PeriodoFacturacion periodo) {
        
        // Calcular precio proporcional
        BigDecimal proporcion = new BigDecimal(periodo.getDiasEfectivos())
            .divide(new BigDecimal(periodo.getDiasDelMes()), 4, java.math.RoundingMode.HALF_UP);
        
        BigDecimal precioProporcional = precioMensual.multiply(proporcion)
            .setScale(2, java.math.RoundingMode.HALF_UP);
        
        // Generar descripción con período parcial
        String descripcionCompleta = String.format("%s (%s)",
            descripcionBase,
            periodo.generarDescripcionPeriodo()
        );
        
        // Crear item con precio proporcional
        return new ItemFactura(descripcionCompleta, precioProporcional, cantidad, alicuotaIVA);
    }

    /**
     * Obtiene el valor decimal de la alícuota de IVA.
     * @return Valor decimal de la alícuota (ej: 0.21 para IVA_21)
     */
    private BigDecimal obtenerValorAlicuota() {
        return switch (this.alicuotaIVA) {
            case IVA_21 -> new BigDecimal("0.21");
            case IVA_10_5 -> new BigDecimal("0.105");
            case IVA_27 -> new BigDecimal("0.27");
            case IVA_2_5 -> new BigDecimal("0.025");
            case EXENTO -> BigDecimal.ZERO;
        };
    }
}
