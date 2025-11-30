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
 * Responsabilidades:
 * - Almacenar datos de la asociación Pago-Factura
 * - Validar la integridad de los datos (montos, referencias no nulas, etc.)
 * - Garantizar que el monto aplicado no exceda el saldo pendiente de la factura
 * 
 * NO es responsable de:
 * - Lógica de negocio sobre tipos de pago (total/parcial) → Ver Factura
 * - Cálculos de porcentajes y distribuciones → Ver Pago y Factura
 * - Gestión del flujo de pagos → Ver PagoService
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
        DetallePago detalle = new DetallePago(pago, factura, montoAplicado);
        
        // Mantener coherencia bidireccional después de construir el objeto
        pago.agregarDetallePago(detalle);
        factura.agregarDetallePago(detalle);
        
        return detalle;
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
}
