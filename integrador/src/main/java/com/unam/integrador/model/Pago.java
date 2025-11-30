package com.unam.integrador.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.unam.integrador.model.enums.MetodoPago;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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
    
    /**
     * Detalles de pago que indican a qué facturas se aplicó este pago.
     * Un pago puede aplicarse a múltiples facturas (pago combinado).
     */
    @OneToMany(mappedBy = "pago", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<DetallePago> detallesPago = new ArrayList<>();

    /**
     * Número de recibo asociado a este pago.
     * Permite agrupar múltiples pagos bajo un mismo recibo (pagos combinados).
     * El recibo se genera dinámicamente, no se persiste como entidad.
     */
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
     * Asocia este pago a una factura creando un DetallePago.
     * Mantiene la consistencia bidireccional de la relación.
     * 
     * @param factura La factura a la que se asocia el pago
     * @param montoAplicado El monto específico del pago que se aplica a esta factura
     * @return El DetallePago creado
     * @throws IllegalArgumentException si la factura es nula o el monto es inválido
     */
    public DetallePago asociarFactura(Factura factura, BigDecimal montoAplicado) {
        if (factura == null) {
            throw new IllegalArgumentException("La factura no puede ser nula");
        }
        
        // Crear el detalle de pago usando el factory method (modelo RICO)
        DetallePago detalle = DetallePago.crear(this, factura, montoAplicado);
        
        // Agregar a la lista de detalles
        this.detallesPago.add(detalle);
        
        return detalle;
    }
    
    /**
     * Obtiene todas las facturas asociadas a este pago.
     * 
     * @return Lista de facturas (puede estar vacía)
     */
    public List<Factura> obtenerFacturas() {
        return detallesPago.stream()
            .map(DetallePago::getFactura)
            .toList();
    }
    
    /**
     * Calcula el monto total aplicado de este pago.
     * Suma todos los montos aplicados en los detalles de pago.
     * 
     * @return Monto total aplicado
     */
    public BigDecimal calcularMontoTotalAplicado() {
        return detallesPago.stream()
            .map(DetallePago::getMontoAplicado)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Calcula el monto restante del pago (no aplicado a facturas).
     * 
     * @return Monto restante
     */
    public BigDecimal calcularMontoRestante() {
        return this.monto.subtract(calcularMontoTotalAplicado());
    }
    
    /**
     * Verifica si el pago ha sido completamente aplicado a facturas.
     * 
     * @return true si todo el monto fue aplicado
     */
    public boolean estaCompletamenteAplicado() {
        return calcularMontoRestante().compareTo(BigDecimal.ZERO) == 0;
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
     * Setter package-private para asignar el número de recibo cuando un recibo
     * agrupa varios pagos (pagos combinados). No expuesto públicamente.
     */
    public void setNumeroRecibo(String numeroRecibo) {
        this.numeroRecibo = numeroRecibo;
    }
}
