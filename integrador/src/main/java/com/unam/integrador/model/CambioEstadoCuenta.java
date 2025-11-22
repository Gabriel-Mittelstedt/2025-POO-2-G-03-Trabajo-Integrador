package com.unam.integrador.model;

import java.time.LocalDateTime;

import com.unam.integrador.model.enums.EstadoCuenta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

/**
 * Entidad que registra el historial de cambios de estado de una cuenta de cliente.
 * 
 * <p>Esta clase mantiene una auditoría completa de todos los cambios de estado
 * que ha tenido una cuenta, permitiendo rastrear cuándo, cómo y por qué cambió
 * el estado de un cliente.</p>
 * 
 * <p>Cada registro incluye:</p>
 * <ul>
 *   <li>Estado anterior y nuevo estado</li>
 *   <li>Fecha y hora exacta del cambio</li>
 *   <li>Motivo del cambio (obligatorio)</li>
 *   <li>Referencia al cliente afectado</li>
 * </ul>
 */
@Data
@Entity
public class CambioEstadoCuenta {
    
    /**
     * Identificador único del cambio de estado.
     * Generado automáticamente por la base de datos.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;
    
    /**
     * Cliente cuyo estado fue modificado.
     * 
     * <p>Relación many-to-one: un cliente puede tener múltiples cambios de estado
     * a lo largo del tiempo.</p>
     */
    @NotNull(message = "El cliente es obligatorio")
    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private CuentaCliente cliente;
    
    /**
     * Estado anterior de la cuenta antes del cambio.
     * 
     * <p>Puede ser null si es el primer cambio de estado registrado
     * (cuando el estado inicial era el por defecto).</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 20)
    private EstadoCuenta estadoAnterior;
    
    /**
     * Nuevo estado de la cuenta después del cambio.
     * 
     * <p>Este campo siempre debe tener un valor válido.</p>
     */
    @NotNull(message = "El estado nuevo es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private EstadoCuenta estadoNuevo;
    
    /**
     * Fecha y hora exacta en que se realizó el cambio de estado.
     * 
     * <p>Se establece automáticamente al momento de persistir el registro.</p>
     */
    @NotNull(message = "La fecha del cambio es obligatoria")
    @Column(name = "fecha_cambio", nullable = false)
    private LocalDateTime fechaCambio;
    
    /**
     * Motivo o justificación del cambio de estado.
     * 
     * <p>Campo obligatorio que debe explicar la razón del cambio.</p>
     * <p>Ejemplos:</p>
     * <ul>
     *   <li>"Mora en el pago de 3 meses consecutivos"</li>
     *   <li>"Solicitud del cliente"</li>
     *   <li>"Regularización de deuda pendiente"</li>
     *   <li>"Cierre definitivo de cuenta"</li>
     * </ul>
     */
    @NotBlank(message = "El motivo del cambio es obligatorio")
    @Size(min = 5, max = 500, message = "El motivo debe tener entre 5 y 500 caracteres")
    @Column(nullable = false, length = 500)
    private String motivo;
    
    /**
     * Callback ejecutado antes de persistir la entidad en la base de datos.
     * 
     * <p>Establece automáticamente la fecha y hora del cambio al momento actual
     * si no fue especificada previamente.</p>
     */
    @PrePersist
    public void prePersist() {
        if (fechaCambio == null) {
            fechaCambio = LocalDateTime.now();
        }
    }
}
