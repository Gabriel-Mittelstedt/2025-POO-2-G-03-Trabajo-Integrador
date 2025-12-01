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
import lombok.ToString;

/**
 * Entidad que representa una cuenta de cliente en el sistema ERP de facturación.
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
     */
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    @Column(nullable = false, length = 100)
    private String nombre; 
    
    /**
     * Razón social o nombre legal de la empresa.
     */
    @NotBlank(message = "La razón social es obligatoria")
    @Size(min = 2, max = 150, message = "La razón social debe tener entre 2 y 150 caracteres")
    @Column(nullable = false, length = 150)
    private String razonSocial; 
    
    /**
     * Número de CUIT o DNI del cliente.
     */
    @NotBlank(message = "El CUIT/DNI es obligatorio")
    @Pattern(regexp = "^\\d{7,11}$", message = "El CUIT debe tener 11 dígitos o el DNI 7-8 dígitos")
    @Column(unique = true, nullable = false, name = "cuit_dni", length = 11)
    private String cuitDni; 
    
    /**
     * Dirección completa del cliente.
     */
    @NotBlank(message = "El domicilio es obligatorio")
    @Size(min = 5, max = 200, message = "El domicilio debe tener entre 5 y 200 caracteres")
    @Column(nullable = false, length = 200)
    private String domicilio; 
    
    /**
     * Dirección de correo electrónico del cliente.
     */
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email no es válido")
    @Size(max = 100, message = "El email no debe superar los 100 caracteres")
    @Column(nullable = false, length = 100)
    private String email; 
    
    /**
     * Número de teléfono de contacto del cliente.
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
     * Saldo actual de la cuenta del cliente, este representa el balance entre facturas emitidas y pagos recibidos.
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal saldo;
    
    /**
     * Lista de servicios contratados por este cliente.
     */
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServicioContratado> serviciosContratados = new ArrayList<>();
    
    /**
     * Historial de cambios de estado de la cuenta.
     */
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<CambioEstadoCuenta> historialCambiosEstado = new ArrayList<>();
    
    /**
     * Callback ejecutado antes de persistir la entidad en la base de datos.
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
     * Verifica si la cuenta está en un estado que permite facturación, solo se puede facturar a clientes
     * cuya cuenta esté en estado ACTIVA.
     * @return true si se puede facturar al cliente, false en caso contrario
     */
    public boolean puedeFacturar() {
        return this.estado == EstadoCuenta.ACTIVA;
    }

        
    /**
     * Obtiene el saldo a favor del cliente.
     * Un saldo negativo indica que el cliente tiene crédito a favor.
     * @return el saldo a favor (valor absoluto si es negativo, cero si no tiene)
     */
    public BigDecimal getSaldoAFavor() {
        if (saldo == null) {
            return BigDecimal.ZERO;
        }
        // Si el saldo es negativo, el cliente tiene crédito a favor
        return saldo.compareTo(BigDecimal.ZERO) < 0 ? saldo.abs() : BigDecimal.ZERO;
    }
    
    /**
     * Verifica si el cliente tiene saldo a favor disponible.
     * @return true si tiene saldo a favor, false en caso contrario
     */
    public boolean tieneSaldoAFavor() {
        return saldo != null && saldo.compareTo(BigDecimal.ZERO) < 0;
    }
    
    /**
     * Aplica saldo a favor del cliente a un monto específico.
     * Reduce el saldo a favor (lo acerca a cero) y retorna el monto aplicado.
     * 
     * @param montoSolicitado el monto que se desea aplicar
     * @return el monto efectivamente aplicado (puede ser menor si no hay suficiente saldo)
     * @throws IllegalArgumentException si el monto solicitado es inválido
     */
    public BigDecimal aplicarSaldoAFavor(BigDecimal montoSolicitado) {
        if (montoSolicitado == null || montoSolicitado.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto a aplicar debe ser mayor a cero");
        }
        
        BigDecimal saldoDisponible = getSaldoAFavor();
        if (saldoDisponible.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalStateException("El cliente no tiene saldo a favor disponible");
        }
        
        // Determinar cuánto se puede aplicar
        BigDecimal montoAplicado = montoSolicitado.min(saldoDisponible);
        
        // Actualizar el saldo (reducir el crédito a favor)
        this.saldo = this.saldo.add(montoAplicado);
        
        return montoAplicado;
    }
    
    /**
     * Registra un saldo a favor generado por un pago excedente.
     * 
     * @param monto el monto del excedente
     * @throws IllegalArgumentException si el monto es inválido
     */
    public void registrarSaldoAFavor(BigDecimal monto) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del saldo a favor debe ser mayor a cero");
        }
        
        // Restar el monto del saldo (hacer más negativo = más crédito a favor)
        if (this.saldo == null) {
            this.saldo = BigDecimal.ZERO;
        }
        this.saldo = this.saldo.subtract(monto);
    }
}
