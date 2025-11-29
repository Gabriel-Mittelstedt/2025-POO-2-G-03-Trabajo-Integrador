package com.unam.integrador.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import com.unam.integrador.model.enums.MetodoPago;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Entidad Recibo con comportamiento de dominio (Modelo Rico).
 * Contiene validaciones y métodos para asociarse a un Pago.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Para JPA
public class Recibo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long IDRecibo;

    @Column(nullable = false, unique = true)
    private String numero;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MetodoPago metodoPago;

    @Column(length = 500)
    private String referencia;

    @Column(length = 1000)
    private String facturasAsociadas;

    // --- Relaciones ---

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pago_id", nullable = false)
    private Pago pago;

    // Constructor privado para creación controlada
    private Recibo(String numero, BigDecimal monto, MetodoPago metodoPago, String referencia, String facturasAsociadas) {
        validarNumero(numero);
        validarMonto(monto);
        validarMetodoPago(metodoPago);
        validarReferencia(referencia);
        validarFacturasAsociadas(facturasAsociadas);

        this.numero = numero;
        this.fecha = LocalDate.now();
        this.monto = monto;
        this.metodoPago = metodoPago;
        this.referencia = referencia;
        this.facturasAsociadas = facturasAsociadas;
    }

    /**
     * Factory method para crear un Recibo validado.
     */
    public static Recibo crearRecibo(String numero, BigDecimal monto, MetodoPago metodoPago, String referencia, String facturasAsociadas) {
        return new Recibo(numero, monto, metodoPago, referencia, facturasAsociadas);
    }

    /**
     * Asocia este recibo a un pago y mantiene la consistencia bidireccional.
     */
    public void asociarPago(Pago pago) {
        if (pago == null) {
            throw new IllegalArgumentException("El pago no puede ser nulo");
        }
        if (this.pago != null) {
            throw new IllegalStateException("Este recibo ya tiene un pago asociado");
        }
        this.pago = pago;
        // Asegurar la asociación recíproca en Pago
        if (pago.getRecibo() == null) {
            pago.setRecibo(this); // método package-private en Pago
        }
    }

    // --- Validaciones privadas ---
    private void validarNumero(String numero) {
        if (numero == null || numero.trim().isEmpty()) {
            throw new IllegalArgumentException("El número del recibo es obligatorio");
        }
        if (numero.length() > 50) {
            throw new IllegalArgumentException("El número del recibo es demasiado largo");
        }
    }

    private void validarMonto(BigDecimal monto) {
        if (monto == null) {
            throw new IllegalArgumentException("El monto del recibo no puede ser nulo");
        }
        if (monto.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del recibo debe ser mayor a cero");
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

    private void validarMetodoPago(MetodoPago metodoPago) {
        if (metodoPago == null) {
            throw new IllegalArgumentException("El método de pago del recibo no puede ser nulo");
        }
    }

    private void validarReferencia(String referencia) {
        if (referencia != null && referencia.length() > 500) {
            throw new IllegalArgumentException("La referencia no puede exceder 500 caracteres");
        }
    }

    private void validarFacturasAsociadas(String facturasAsociadas) {
        if (facturasAsociadas != null && facturasAsociadas.length() > 1000) {
            throw new IllegalArgumentException("La descripción de facturas asociadas es demasiado larga");
        }
    }

    // Setter package-private para JPA / dominio
    void setPago(Pago pago) {
        this.pago = pago;
    }
}
