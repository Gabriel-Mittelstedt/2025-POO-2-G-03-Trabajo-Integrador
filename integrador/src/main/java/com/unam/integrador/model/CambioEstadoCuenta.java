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
 */
@Data
@Entity
public class CambioEstadoCuenta {
    
    /**
     * Identificador único del cambio de estado.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;
    
    /**
     * Cliente cuyo estado fue modificado.
     */
    @NotNull(message = "El cliente es obligatorio")
    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private CuentaCliente cliente;
    
    /**
     * Estado anterior de la cuenta antes del cambio, este puede ser null si es el primer cambio de estado registrado.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 20)
    private EstadoCuenta estadoAnterior;
    
    /**
     * Nuevo estado de la cuenta después del cambio.
     */
    @NotNull(message = "El estado nuevo es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private EstadoCuenta estadoNuevo;
    
    /**
     * Fecha y hora exacta en que se realizó el cambio de estado, esta se establece automáticamente al momento de persistir el registro
     */
    @NotNull(message = "La fecha del cambio es obligatoria")
    @Column(name = "fecha_cambio", nullable = false)
    private LocalDateTime fechaCambio;
    
    /**
     * Motivo o justificación del cambio de estado.
     */
    @NotBlank(message = "El motivo del cambio es obligatorio")
    @Size(min = 5, max = 500, message = "El motivo debe tener entre 5 y 500 caracteres")
    @Column(nullable = false, length = 500)
    private String motivo;
    
    /**
     * Callback ejecutado antes de persistir la entidad en la base de datos.
     */
    @PrePersist
    public void prePersist() {
        if (fechaCambio == null) {
            fechaCambio = LocalDateTime.now();
        }
    }
}
