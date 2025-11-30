package com.unam.integrador.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Entidad de asociación entre Pago y Factura (Clase Intermedia).
 * 
 * Representa la relación muchos-a-muchos entre Pago y Factura:
 * - Un pago puede aplicarse a múltiples facturas
 * - Una factura puede pagarse con múltiples pagos (pagos parciales)
 * 
 * Esta clase almacena información específica de la relación:
 * - Monto específico del pago aplicado a esta factura
 * - Fecha y hora de aplicación del pago
 * 
 * Implementa el patrón de Modelo Rico (Rich Domain Model):
 * - Constructor privado con factory methods
 * - Validaciones de reglas de negocio
 * - Encapsulación completa
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Solo para JPA
public class DetallePago {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Pago del cual se toma el monto.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pago_id", nullable = false)
    private Pago pago;
    
    /**
     * Factura a la que se aplica este pago.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id", nullable = false)
    private Factura factura;
    
    /**
     * Monto específico aplicado de este pago a esta factura.
     * En caso de pago total, será igual al total de la factura.
     * En caso de pago parcial, será menor al total de la factura.
     * En caso de pago combinado (múltiples facturas), será la porción asignada.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montoAplicado;
    
    /**
     * Fecha y hora en que se aplicó este pago a la factura.
     * Útil para auditoría y trazabilidad.
     */
    @Column(nullable = false)
    private LocalDateTime fechaAplicacion;
    
    // --- Constructor privado (fuerza uso de factory methods) ---
    
    /**
     * Constructor privado que inicializa un DetallePago.
     * Solo accesible a través del factory method estático.
     * 
     * @param pago Pago que se aplica
     * @param factura Factura a la que se aplica el pago
     * @param montoAplicado Monto específico aplicado
     */
    private DetallePago(Pago pago, Factura factura, BigDecimal montoAplicado) {
        validarPago(pago);
        validarFactura(factura);
        validarMontoAplicado(montoAplicado);
        validarMontoContraSaldoPendiente(montoAplicado, factura);
        
        this.pago = pago;
        this.factura = factura;
        this.montoAplicado = montoAplicado;
        this.fechaAplicacion = LocalDateTime.now();
    }
    
    // --- Factory Methods (Métodos de Fábrica) ---
    
    /**
     * Crea un nuevo DetallePago validando todas las reglas de negocio.
     * 
     * @param pago Pago que se aplica (no puede ser nulo)
     * @param factura Factura a la que se aplica (no puede ser nula)
     * @param montoAplicado Monto específico aplicado (debe ser > 0 y <= saldoPendiente)
     * @return Nueva instancia de DetallePago validada
     * @throws IllegalArgumentException si alguna validación falla
     */
    public static DetallePago crear(Pago pago, Factura factura, BigDecimal montoAplicado) {
        return new DetallePago(pago, factura, montoAplicado);
    }
    
    // --- Métodos de Dominio (Comportamiento) ---
    
    /**
     * Verifica si este detalle representa un pago total de la factura.
     * 
     * @return true si el monto aplicado cubre el saldo pendiente de la factura
     */
    public boolean esPagoTotal() {
        if (factura == null || factura.getSaldoPendiente() == null) {
            return false;
        }
        return montoAplicado.compareTo(factura.getSaldoPendiente()) >= 0;
    }
    
    /**
     * Verifica si este detalle representa un pago parcial de la factura.
     * 
     * @return true si el monto aplicado es menor al saldo pendiente de la factura
     */
    public boolean esPagoParcial() {
        if (factura == null || factura.getSaldoPendiente() == null) {
            return false;
        }
        return montoAplicado.compareTo(factura.getSaldoPendiente()) < 0;
    }
    
    /**
     * Calcula el porcentaje del pago total que representa este monto aplicado.
     * 
     * @return Porcentaje (0-100) del pago total
     */
    public BigDecimal calcularPorcentajeDelPagoTotal() {
        if (pago == null || pago.getMonto() == null || pago.getMonto().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return montoAplicado
            .multiply(BigDecimal.valueOf(100))
            .divide(pago.getMonto(), 2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calcula el porcentaje del total de la factura que representa este pago.
     * 
     * @return Porcentaje (0-100) del total de la factura
     */
    public BigDecimal calcularPorcentajeDelTotalFactura() {
        if (factura == null || factura.getTotal() == null || factura.getTotal().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return montoAplicado
            .multiply(BigDecimal.valueOf(100))
            .divide(factura.getTotal(), 2, RoundingMode.HALF_UP);
    }
    
    // --- Métodos de Validación Privados ---
    
    /**
     * Valida que el pago no sea nulo.
     * 
     * @param pago El pago a validar
     * @throws IllegalArgumentException si el pago es nulo
     */
    private void validarPago(Pago pago) {
        if (pago == null) {
            throw new IllegalArgumentException("El pago no puede ser nulo");
        }
    }
    
    /**
     * Valida que la factura no sea nula.
     * 
     * @param factura La factura a validar
     * @throws IllegalArgumentException si la factura es nula
     */
    private void validarFactura(Factura factura) {
        if (factura == null) {
            throw new IllegalArgumentException("La factura no puede ser nula");
        }
    }
    
    /**
     * Valida que el monto aplicado sea mayor a cero y tenga formato válido.
     * 
     * @param montoAplicado El monto a validar
     * @throws IllegalArgumentException si el monto es inválido
     */
    private void validarMontoAplicado(BigDecimal montoAplicado) {
        if (montoAplicado == null) {
            throw new IllegalArgumentException("El monto aplicado no puede ser nulo");
        }
        
        if (montoAplicado.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto aplicado debe ser mayor a cero");
        }
        
        // Validación para respetar la definición de la columna: precision=10, scale=2
        final int maxPrecision = 10;
        final int maxScale = 2;
        
        if (montoAplicado.scale() > maxScale) {
            throw new IllegalArgumentException(
                String.format("Monto inválido: máximo %d decimales permitidos (ej: 12345.67).", maxScale));
        }
        
        // Comprobar dígitos enteros (precision - scale) para evitar overflow en DB numeric(10,2)
        final int maxIntegerDigits = maxPrecision - maxScale; // 8
        int integerDigits = montoAplicado.abs().setScale(0, RoundingMode.DOWN).toBigInteger().toString().length();
        
        if (integerDigits > maxIntegerDigits) {
            throw new IllegalArgumentException(
                String.format(
                    "Monto inválido: la parte entera no puede tener más de %d dígitos. " +
                    "Valor máximo permitido: 99,999,999.99.",
                    maxIntegerDigits));
        }
    }
    
    /**
     * Valida que el monto aplicado no exceda el saldo pendiente de la factura.
     * 
     * @param montoAplicado Monto a aplicar
     * @param factura Factura a la que se aplica
     * @throws IllegalArgumentException si el monto excede el saldo pendiente
     */
    private void validarMontoContraSaldoPendiente(BigDecimal montoAplicado, Factura factura) {
        if (factura.getSaldoPendiente() == null) {
            throw new IllegalStateException("La factura no tiene saldo pendiente calculado");
        }
        
        if (montoAplicado.compareTo(factura.getSaldoPendiente()) > 0) {
            throw new IllegalArgumentException(
                String.format(
                    "El monto aplicado ($%s) no puede exceder el saldo pendiente de la factura ($%s)",
                    montoAplicado, factura.getSaldoPendiente()));
        }
    }
    
    // --- Setters controlados solo para uso interno/JPA ---
    
    /**
     * Setter package-private para el pago (usado por JPA y métodos de dominio).
     */
    void setPago(Pago pago) {
        this.pago = pago;
    }
    
    /**
     * Setter package-private para la factura (usado por JPA y métodos de dominio).
     */
    void setFactura(Factura factura) {
        this.factura = factura;
    }
}
