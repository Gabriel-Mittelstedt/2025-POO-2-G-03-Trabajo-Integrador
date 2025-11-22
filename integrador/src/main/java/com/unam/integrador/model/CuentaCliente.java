package com.unam.integrador.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.unam.integrador.model.enums.EstadoCuenta;
import com.unam.integrador.model.enums.TipoCondicionIVA;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

/**
 * Entidad que representa una cuenta de cliente en el sistema ERP de facturación.
 * 
 * <p>Esta clase gestiona la información completa de un cliente, incluyendo sus datos
 * personales, fiscales y el estado de su cuenta. Cada cliente puede tener múltiples
 * servicios contratados asociados.</p>
 */
@Data
@Entity
public class CuentaCliente {
    
    /**
     * Identificador único de la cuenta de cliente.
     * Generado automáticamente por la base de datos.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;
    
    /**
     * Nombre completo del cliente.
     * 
     * <p>Campo obligatorio con longitud entre 2 y 100 caracteres.</p>
     * <p>Ejemplos: "Juan Pérez", "María González SA"</p>
     */
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    @Column(nullable = false, length = 100)
    private String nombre; 
    
    /**
     * Razón social o nombre legal de la empresa.
     * 
     * <p>Campo obligatorio con longitud entre 2 y 150 caracteres.</p>
     * <p>Representa el nombre oficial registrado ante la AFIP.</p>
     */
    @NotBlank(message = "La razón social es obligatoria")
    @Size(min = 2, max = 150, message = "La razón social debe tener entre 2 y 150 caracteres")
    @Column(nullable = false, length = 150)
    private String razonSocial; 
    
    /**
     * Número de CUIT o DNI del cliente.
     * 
     * <p>Campo obligatorio y único en el sistema.</p>
     * <p>Formatos válidos:</p>
     * <ul>
     *   <li>CUIT: 11 dígitos (ej: 20123456789)</li>
     *   <li>DNI: 7-8 dígitos (ej: 12345678)</li>
     * </ul>
     * <p>Se almacena sin guiones ni espacios.</p>
     */
    @NotBlank(message = "El CUIT/DNI es obligatorio")
    @Pattern(regexp = "^\\d{7,11}$", message = "El CUIT debe tener 11 dígitos o el DNI 7-8 dígitos")
    @Column(unique = true, nullable = false, name = "cuit_dni", length = 11)
    private String cuitDni; 
    
    /**
     * Dirección completa del cliente.
     * 
     * <p>Campo obligatorio con longitud entre 5 y 200 caracteres.</p>
     */
    @NotBlank(message = "El domicilio es obligatorio")
    @Size(min = 5, max = 200, message = "El domicilio debe tener entre 5 y 200 caracteres")
    @Column(nullable = false, length = 200)
    private String domicilio; 
    
    /**
     * Dirección de correo electrónico del cliente.
     * 
     * <p>Campo obligatorio con validación de formato email.</p>
     * <p>Se utiliza para envío de facturas y notificaciones.</p>
     * <p>Longitud máxima: 100 caracteres.</p>
     */
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email no es válido")
    @Size(max = 100, message = "El email no debe superar los 100 caracteres")
    @Column(nullable = false, length = 100)
    private String email; 
    
    /**
     * Número de teléfono de contacto del cliente.
     * 
     * <p>Campo opcional con longitud máxima de 20 caracteres.</p>
     * <p>Puede incluir código de área y característica.</p>
     * <p>Ejemplo: "+54 11 1234-5678"</p>
     */
    @Size(max = 20, message = "El teléfono no debe superar los 20 caracteres")
    @Column(length = 20)
    private String telefono; 
    
    /**
     * Condición del cliente frente al IVA.
     * @see TipoCondicionIVA
     */
    @NotNull(message = "La condición de IVA es obligatoria")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoCondicionIVA condicionIva; 
    
    /**
     * Estado actual de la cuenta del cliente.
     * @see EstadoCuenta
     */
    @NotNull(message = "El estado de cuenta es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCuenta estado; 
    
    /**
     * Saldo actual de la cuenta del cliente.
     * 
     * <p>Representa el balance entre facturas emitidas y pagos recibidos.</p>
     * <p>Características:</p>
     * <ul>
     *   <li>Precisión: 10 dígitos totales, 2 decimales</li>
     *   <li>Valor inicial: 0.00 (establecido automáticamente)</li>
     *   <li>Positivo: cliente debe dinero</li>
     *   <li>Negativo: cliente tiene saldo a favor</li>
     * </ul>
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal saldo;
    
    // --- Relaciones ---
    
    /**
     * Lista de servicios contratados por este cliente.
     */
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServicioContratado> serviciosContratados = new ArrayList<>();
    
    /**
     * Historial de cambios de estado de la cuenta.
     * 
     * <p>Mantiene un registro completo de todos los cambios de estado realizados,
     * incluyendo fecha, estado anterior, estado nuevo y motivo del cambio.</p>
     */
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CambioEstadoCuenta> historialCambiosEstado = new ArrayList<>();
    
    /**
     * Callback ejecutado antes de persistir la entidad en la base de datos.
     * 
     * <p>Inicializa valores por defecto:</p>
     * <ul>
     *   <li>Estado: ACTIVA (si no fue especificado)</li>
     *   <li>Saldo: 0.00 (si no fue especificado)</li>
     * </ul>
     * 
     * <p>Este método es invocado automáticamente por JPA antes del primer INSERT.</p>
     */
    @PrePersist
    public void prePersist() {
        if (estado == null) {
            estado = EstadoCuenta.ACTIVA;
        }
        if (saldo == null) {
            saldo = BigDecimal.ZERO;
        }
    }
    
    // --- Métodos de negocio (Modelo Rico) ---
    
    /**
     * Contrata un servicio para este cliente.
     * Registra la fecha actual y el precio actual del servicio.
     * 
     * @param servicio el servicio a contratar
     * @throws IllegalArgumentException si el servicio ya está contratado activamente
     */
    public void contratarServicio(Servicio servicio) {
        if (tieneServicioContratadoActivo(servicio)) {
            throw new IllegalArgumentException("El servicio '" + servicio.getNombre() + "' ya está contratado para este cliente.");
        }
        
        ServicioContratado servicioContratado = new ServicioContratado();
        servicioContratado.setCliente(this);
        servicioContratado.setServicio(servicio);
        servicioContratado.setFechaAlta(LocalDate.now());
        servicioContratado.setPrecioContratado(servicio.getPrecio());
        servicioContratado.setActivo(true);
        
        this.serviciosContratados.add(servicioContratado);
    }
    
    /**
     * Verifica si el servicio especificado está actualmente contratado y activo.
     * 
     * @param servicio el servicio a verificar
     * @return true si el servicio está contratado y activo, false en caso contrario
     */
    public boolean tieneServicioContratadoActivo(Servicio servicio) {
        return this.serviciosContratados.stream()
            .anyMatch(sc -> sc.getServicio().equals(servicio) && sc.getActivo());
    }
    
    /**
     * Obtiene la lista de servicios contratados que están activos.
     * 
     * @return lista de servicios contratados activos
     */
    public List<ServicioContratado> getServiciosContratadosActivos() {
        return this.serviciosContratados.stream()
            .filter(ServicioContratado::getActivo)
            .toList();
    }
    
    /**
     * Desvincula un servicio del cliente (baja lógica).
     * El servicio no se facturará en futuros períodos.
     * 
     * @param servicio el servicio a desvincular
     * @throws IllegalArgumentException si el servicio no está contratado activamente
     */
    public void desvincularServicio(Servicio servicio) {
        ServicioContratado contrato = this.serviciosContratados.stream()
            .filter(sc -> sc.getServicio().equals(servicio) && sc.getActivo())
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "El servicio '" + servicio.getNombre() + "' no está contratado activamente"));
        
        contrato.desvincular();
    }
    
    /**
     * Actualiza los datos modificables del cliente.
     * 
     * <p>Este método permite modificar todos los campos del cliente excepto el CUIT/DNI,
     * que es inmutable por ser el identificador fiscal único.</p>
     * 
     * @param nombre nuevo nombre del cliente
     * @param razonSocial nueva razón social
     * @param domicilio nuevo domicilio
     * @param email nuevo email
     * @param telefono nuevo teléfono (puede ser null)
     * @param condicionIva nueva condición de IVA
     * @throws IllegalArgumentException si algún dato es inválido
     */
    public void actualizarDatos(String nombre, String razonSocial, String domicilio, 
                                String email, String telefono, TipoCondicionIVA condicionIva) {
        // Validaciones básicas (Bean Validation ya valida en el controller, pero reforzamos)
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (razonSocial == null || razonSocial.trim().isEmpty()) {
            throw new IllegalArgumentException("La razón social es obligatoria");
        }
        if (domicilio == null || domicilio.trim().isEmpty()) {
            throw new IllegalArgumentException("El domicilio es obligatorio");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("El email es obligatorio");
        }
        if (condicionIva == null) {
            throw new IllegalArgumentException("La condición de IVA es obligatoria");
        }
        
        this.nombre = nombre.trim();
        this.razonSocial = razonSocial.trim();
        this.domicilio = domicilio.trim();
        this.email = email.trim();
        this.telefono = telefono != null ? telefono.trim() : null;
        this.condicionIva = condicionIva;
    }
    
    /**
     * Cambia el estado de la cuenta del cliente y registra el cambio en el historial.
     * 
     * <p>Este método implementa la lógica de negocio para cambiar el estado de una cuenta,
     * validando los datos y creando automáticamente un registro de auditoría del cambio.</p>
     * 
     * <p>Reglas de negocio aplicadas:</p>
     * <ul>
     *   <li>El motivo del cambio es obligatorio para todos los cambios de estado</li>
     *   <li>Se permite cambiar de cualquier estado a cualquier otro estado</li>
     *   <li>Se registra el estado anterior, el nuevo estado y la fecha/hora del cambio</li>
     *   <li>El cambio se persiste en el historial para auditoría</li>
     * </ul>
     * 
     * @param nuevoEstado el estado al que se desea cambiar la cuenta
     * @param motivo la justificación o razón del cambio (mínimo 5 caracteres, máximo 500)
     * @throws IllegalArgumentException si el nuevo estado es null, si el motivo es inválido,
     *                                  o si se intenta cambiar al mismo estado actual
     */
    public void cambiarEstado(EstadoCuenta nuevoEstado, String motivo) {
        // Validar que el nuevo estado no sea nulo
        if (nuevoEstado == null) {
            throw new IllegalArgumentException("El nuevo estado es obligatorio");
        }
        
        // Validar que el motivo no sea nulo ni vacío
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IllegalArgumentException("El motivo del cambio es obligatorio");
        }
        
        // Validar longitud del motivo
        if (motivo.trim().length() < 5) {
            throw new IllegalArgumentException("El motivo debe tener al menos 5 caracteres");
        }
        
        if (motivo.trim().length() > 500) {
            throw new IllegalArgumentException("El motivo no debe superar los 500 caracteres");
        }
        
        // Validar que el nuevo estado sea diferente al actual
        if (this.estado == nuevoEstado) {
            throw new IllegalArgumentException(
                "El estado de la cuenta ya es " + nuevoEstado.getDescripcion() + ". No es necesario realizar el cambio."
            );
        }
        
        // Crear el registro de cambio de estado para el historial
        CambioEstadoCuenta cambio = new CambioEstadoCuenta();
        cambio.setCliente(this);
        cambio.setEstadoAnterior(this.estado);
        cambio.setEstadoNuevo(nuevoEstado);
        cambio.setMotivo(motivo.trim());
        
        // Agregar el cambio al historial
        this.historialCambiosEstado.add(cambio);
        
        // Actualizar el estado actual de la cuenta
        this.estado = nuevoEstado;
    }
    
    /**
     * Verifica si la cuenta está en un estado que permite facturación.
     * 
     * <p>Según las reglas de negocio, solo se puede facturar a clientes
     * cuya cuenta esté en estado ACTIVA.</p>
     * 
     * @return true si se puede facturar al cliente, false en caso contrario
     */
    public boolean puedeFacturar() {
        return this.estado == EstadoCuenta.ACTIVA;
    }
}
