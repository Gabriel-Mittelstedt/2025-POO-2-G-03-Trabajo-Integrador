package com.unam.integrador.model;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Entidad de dominio que representa un Pago realizado sobre una Factura.
 * 

 * Características del modelo rico:
 * - Constructor privado que fuerza el uso de factory methods
 * - Factory methods que validan y establecen el estado inicial correcto
 * - Validaciones de reglas de negocio dentro de la entidad
 * - Métodos de dominio para operaciones complejas

 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Solo para JPA
public class Pago {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPago;
    
    @Column(nullable = false)
    private LocalDate fechaPago;
    
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
    @ToString.Exclude
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
     * Agrega un detalle de pago manteniendo la coherencia bidireccional.
     * Método package-private para uso interno.
     */
    void agregarDetallePago(DetallePago detalle) {
        if (!this.detallesPago.contains(detalle)) {
            this.detallesPago.add(detalle);
        }
    }
    
    /**
     * Calcula el monto total aplicado de este pago.
     * Suma todos los montos aplicados en los detalles de pago.
     * 
     * @return Monto total aplicado
     */
    public BigDecimal calcularMontoTotalAplicado() {
        BigDecimal total = BigDecimal.ZERO;
        
        for (DetallePago detalle : detallesPago) {
            BigDecimal montoDetalle = detalle.getMontoAplicado();
            total = total.add(montoDetalle);
        }
        
        return total;
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

    public void setNumeroRecibo(String numeroRecibo) {
        this.numeroRecibo = numeroRecibo;
    }
}
