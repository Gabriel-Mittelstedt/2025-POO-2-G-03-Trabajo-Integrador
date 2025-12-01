package com.unam.integrador.model;

import java.math.BigDecimal;

import com.unam.integrador.model.enums.TipoAlicuotaIVA;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Representa una línea de detalle en una factura.
 * Modelo RICO: encapsula la lógica de cálculo de precios, IVA y totales.
 * 
 * Cada item representa un servicio facturado con su precio, cantidad,
 * alícuota de IVA y cálculos derivados (subtotal, IVA, total).
 * 
 * Soporta facturación proporcional mediante el factory method crearProporcional().
 * 
 * @author Sistema ERP Facturación
 * @version 1.0
 */
@Data
@Entity
@NoArgsConstructor
public class ItemFactura {
    
    /** Identificador único del item (clave primaria). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- Datos del servicio al momento de facturar ---
    
    /** Descripción del servicio facturado. */
    @Column(nullable = false)
    private String descripcion;

    /** Precio unitario del servicio (puede ser proporcional). */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    /** Cantidad de unidades facturadas (normalmente 1 para servicios). */
    @Column(nullable = false)
    private int cantidad;

    /** Alícuota de IVA aplicable al servicio. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoAlicuotaIVA alicuotaIVA;

    // --- Campos calculados ---
    
    /** Subtotal del item (precio unitario × cantidad). */
    @Column(precision = 10, scale = 2)
    private BigDecimal subtotal;

    /** Monto del IVA calculado según la alícuota. */
    @Column(precision = 10, scale = 2)
    private BigDecimal montoIva;

    /** Total del item (subtotal + IVA). */
    @Column(precision = 10, scale = 2)
    private BigDecimal total;

    // --- Relaciones ---
    
    /** Factura a la que pertenece este item. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id")
    @ToString.Exclude
    private Factura factura;

    /**
     * Constructor para crear un item de factura.
     * Los valores calculados se inicializan en cero y deben calcularse con calcular().
     * 
     * @param descripcion Descripción del servicio
     * @param precioUnitario Precio por unidad
     * @param cantidad Número de unidades
     * @param alicuotaIVA Alícuota de IVA a aplicar
     */
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
     * Factory Method para crear items con facturación proporcional.
     * Calcula automáticamente el precio proporcional basado en los días efectivos del período.
     * 
     * Fórmula: precio_proporcional = precio_mensual × (días_efectivos / días_del_mes)
     * 
     * Ejemplo: Servicio de $10000/mes del 15 al 30 de noviembre (16 días de 30)
     * = $10000 × (16/30) = $5333.33
     * 
     * @param descripcionBase Descripción base del servicio
     * @param precioMensual Precio mensual completo del servicio
     * @param cantidad Cantidad del item (normalmente 1 para servicios)
     * @param alicuotaIVA Alícuota de IVA a aplicar
     * @param periodo Período de facturación con días efectivos calculados
     * @return ItemFactura con precio proporcional y descripción ajustada
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
