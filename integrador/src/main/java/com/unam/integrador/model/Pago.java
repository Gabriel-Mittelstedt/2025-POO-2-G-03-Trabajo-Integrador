package com.unam.integrador.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.unam.integrador.model.enums.MetodoPago;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

/**
 * Entidad de dominio que representa un Pago realizado sobre una Factura.
 * 
 * Implementa el patrón de Modelo Rico (Rich Domain Model) donde la entidad
 * contiene tanto datos como comportamiento/lógica de negocio.
 * 
 * Características del modelo rico:
 * - Constructor privado que fuerza el uso de factory methods
 * - Factory methods que validan y establecen el estado inicial correcto
 * - Validaciones de reglas de negocio dentro de la entidad
 * - Métodos de dominio para operaciones complejas
 * - Encapsulación: solo getters públicos, sin setters directos
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Solo para JPA
public class Pago {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long IDPago;
    
    @Column(nullable = false)
    private LocalDate fechaPago;
    
    @Column(nullable = false)
    private LocalDateTime fechaHoraRegistro;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MetodoPago metodoPago;
    
    @Column(length = 500)
    private String referencia;
    
    // --- Relaciones ---
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id", nullable = false)
    private Factura factura;
    
    @OneToOne(mappedBy = "pago", cascade = CascadeType.ALL, orphanRemoval = true)
    private Recibo recibo;

    @Column(length = 50)
    private String numeroRecibo;
    
    // --- Constructor privado (fuerza uso de factory methods) ---
    
    /**
     * Constructor privado que inicializa un Pago.
     * Solo accesible a través de los factory methods estáticos.
     * 
     * @param monto Monto del pago
     * @param metodoPago Método de pago utilizado
     * @param referencia Referencia o comprobante (opcional)
     */
    private Pago(BigDecimal monto, MetodoPago metodoPago, String referencia) {
        validarMonto(monto);
        validarMetodoPago(metodoPago);
        validarReferencia(referencia);
        
        this.monto = monto;
        this.metodoPago = metodoPago;
        this.referencia = referencia;
        this.fechaPago = LocalDate.now();
        this.fechaHoraRegistro = LocalDateTime.now();
    }
    
    // --- Factory Methods (Métodos de Fábrica) ---
    
    /**
     * Crea un nuevo pago validando todas las reglas de negocio.
     * Este es el método principal para crear instancias de Pago.
     * 
     * @param monto Monto del pago (debe ser mayor a cero)
     * @param metodoPago Método de pago utilizado (no puede ser nulo)
     * @param referencia Referencia del pago, como número de comprobante (opcional)
     * @return Nueva instancia de Pago validada
     * @throws IllegalArgumentException si alguna validación falla
     */
    public static Pago crearPago(BigDecimal monto, MetodoPago metodoPago, String referencia) {
        return new Pago(monto, metodoPago, referencia);
    }
    
    // --- Métodos de Dominio (Comportamiento) ---
    
    /**
     * Asocia este pago a una factura.
     * Mantiene la consistencia bidireccional de la relación.
     * 
     * @param factura La factura a la que se asocia el pago
     * @throws IllegalArgumentException si la factura es nula
     */
    public void asociarFactura(Factura factura) {
        if (factura == null) {
            throw new IllegalArgumentException("La factura no puede ser nula");
        }
        this.factura = factura;
    }
    
    /**
     * Vincula un recibo a este pago.
     * Mantiene la consistencia de la relación uno-a-uno.
     * 
     * @param recibo El recibo generado para este pago
     * @throws IllegalArgumentException si el recibo es nulo
     * @throws IllegalStateException si ya existe un recibo asociado
     */
    public void vincularRecibo(Recibo recibo) {
        if (recibo == null) {
            throw new IllegalArgumentException("El recibo no puede ser nulo");
        }
        if (this.recibo != null) {
            throw new IllegalStateException("Este pago ya tiene un recibo asociado");
        }
        this.recibo = recibo;
    }
    
    /**
     * Verifica si el pago cubre completamente el monto especificado.
     * Útil para validar pagos totales.
     * 
     * @param montoTotal El monto a verificar
     * @return true si el monto del pago es igual o mayor al monto total
     */
    public boolean cubreMonto(BigDecimal montoTotal) {
        if (montoTotal == null) {
            return false;
        }
        return this.monto.compareTo(montoTotal) >= 0;
    }
    
    /**
     * Verifica si el pago es parcial respecto al monto total especificado.
     * 
     * @param montoTotal El monto total a comparar
     * @return true si el pago es menor al monto total
     */
    public boolean esPagoParcial(BigDecimal montoTotal) {
        if (montoTotal == null) {
            return false;
        }
        return this.monto.compareTo(montoTotal) < 0;
    }
    
    /**
     * Verifica si el monto del pago es válido para el saldo pendiente.
     * El pago no debe exceder el saldo pendiente.
     * 
     * @param saldoPendiente El saldo pendiente de la factura
     * @return true si el pago es válido
     */
    public boolean esMontoValido(BigDecimal saldoPendiente) {
        if (saldoPendiente == null) {
            return false;
        }
        // El pago no debe ser mayor al saldo pendiente
        return this.monto.compareTo(saldoPendiente) <= 0;
    }
    
    // --- Métodos de Validación Privados ---
    
    /**
     * Valida que el monto sea mayor a cero.
     * 
     * @param monto El monto a validar
     * @throws IllegalArgumentException si el monto es nulo o menor/igual a cero
     */
    private void validarMonto(BigDecimal monto) {
        if (monto == null) {
            throw new IllegalArgumentException("El monto del pago no puede ser nulo");
        }
        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del pago debe ser mayor a cero");
        }
        // Validación para respetar la definición de la columna: precision=10, scale=2
        final int maxPrecision = 10;
        final int maxScale = 2;
        if (monto.scale() > maxScale) {
            throw new IllegalArgumentException(
                    String.format("Monto inválido: máximo %d decimales permitidos (ej: 12345.67).", maxScale));
        }
        // Comprobar dígitos enteros (precision - scale) para evitar overflow en DB numeric(10,2)
        final int maxIntegerDigits = maxPrecision - maxScale; // 8
        int integerDigits = monto.abs().setScale(0, RoundingMode.DOWN).toBigInteger().toString().length();
        if (integerDigits > maxIntegerDigits) {
            throw new IllegalArgumentException(
                    String.format(
                            "Monto inválido: la parte entera no puede tener más de %d dígitos. " +
                                    "Valor máximo permitido: 99,999,999.99.",
                            maxIntegerDigits));
        }
    }
    
    /**
     * Valida que el método de pago no sea nulo.
     * 
     * @param metodoPago El método de pago a validar
     * @throws IllegalArgumentException si el método de pago es nulo
     */
    private void validarMetodoPago(MetodoPago metodoPago) {
        if (metodoPago == null) {
            throw new IllegalArgumentException("El método de pago no puede ser nulo");
        }
    }
    
    /**
     * Valida que la referencia no exceda el tamaño máximo.
     * 
     * @param referencia La referencia a validar (puede ser nula)
     * @throws IllegalArgumentException si la referencia es demasiado larga
     */
    private void validarReferencia(String referencia) {
        if (referencia != null && referencia.length() > 500) {
            throw new IllegalArgumentException("La referencia no puede exceder 500 caracteres");
        }
    }
    
    // --- Setters controlados solo para uso interno/JPA ---
    // Estos métodos son package-private para permitir su uso desde el service
    // pero evitar el uso indiscriminado desde fuera del paquete
    
    /**
     * Setter interno para la factura (usado por JPA y métodos de dominio).
     */
    void setFactura(Factura factura) {
        this.factura = factura;
    }
    
    /**
     * Setter interno para el recibo (usado por JPA).
     */
    void setRecibo(Recibo recibo) {
        this.recibo = recibo;
    }

    /**
     * Setter package-private para asignar el número de recibo cuando un recibo
     * agrupa varios pagos (pagos combinados). No expuesto públicamente.
     */
    public void setNumeroRecibo(String numeroRecibo) {
        this.numeroRecibo = numeroRecibo;
    }
}
