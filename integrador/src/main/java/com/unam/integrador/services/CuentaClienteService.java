
package com.unam.integrador.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unam.integrador.model.CambioEstadoCuenta;
import com.unam.integrador.model.CuentaCliente;
import com.unam.integrador.model.enums.EstadoCuenta;
import com.unam.integrador.repositories.CambioEstadoCuentaRepository;
import com.unam.integrador.repositories.CuentaClienteRepositorie;
import com.unam.integrador.repositories.ServicioRepository;

/**
 * Servicio para la gestión de cuentas de clientes.
 * Maneja las operaciones CRUD y lógica de negocio.
 */
@Service
@Transactional
public class CuentaClienteService {
    
    @Autowired
    private CuentaClienteRepositorie clienteRepository;
    
    @Autowired
    private ServicioRepository servicioRepository;
    
    @Autowired
    private CambioEstadoCuentaRepository cambioEstadoRepository;
    
    /**
     * Crea un nuevo cliente validando todos los datos
     * @param cliente el cliente a crear
     * @return el cliente creado
     * @throws IllegalArgumentException si hay errores de validación
     */
    public CuentaCliente crearCliente(CuentaCliente cliente) {
        
        // Validar que no exista el CUIT/DNI
        if (clienteRepository.existsByCuitDni(cliente.getCuitDni())) {
            throw new IllegalArgumentException("Ya existe un cliente con el CUIT/DNI: " + cliente.getCuitDni());
        }
        
        return clienteRepository.save(cliente);
    }

    /**
     * Obtiene todos los clientes
     * @return lista de clientes
     */
    @Transactional(readOnly = true)
    public List<CuentaCliente> obtenerTodosLosClientes() {
        return clienteRepository.findAll();
    }
    
    /**
     * Obtiene un cliente por su ID
     * @param id el identificador del cliente
     * @return el cliente encontrado
     * @throws IllegalArgumentException si el cliente no existe
     */
    @Transactional(readOnly = true)
    public CuentaCliente obtenerClientePorId(Long id) {
        return clienteRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado con ID: " + id));
    }
    
    /**
     * Asigna un servicio a un cliente.
     * Registra la fecha actual y el precio actual del servicio.
     * 
     * @param clienteId el ID del cliente
     * @param servicioId el ID del servicio a asignar
     * @return el cliente actualizado
     * @throws IllegalArgumentException si el cliente o servicio no existe, o si ya está asignado
     */
    public CuentaCliente asignarServicio(Long clienteId, Long servicioId) {
        CuentaCliente cliente = obtenerClientePorId(clienteId);
        com.unam.integrador.model.Servicio servicio = servicioRepository.findById(servicioId)
            .orElseThrow(() -> new IllegalArgumentException("Servicio no encontrado con ID: " + servicioId));
        
        // El método contratarServicio ya valida duplicados
        cliente.contratarServicio(servicio);
        
        return clienteRepository.save(cliente);
    }
    
    /**
     * Desvincula un servicio de un cliente.
     * Realiza una baja lógica, el servicio no se incluirá en futuras facturaciones.
     * 
     * @param clienteId el ID del cliente
     * @param servicioId el ID del servicio a desvincular
     * @return el cliente actualizado
     * @throws IllegalArgumentException si el cliente o servicio no existe, o si el servicio no está contratado
     */
    public CuentaCliente desvincularServicio(Long clienteId, Long servicioId) {
        CuentaCliente cliente = obtenerClientePorId(clienteId);
        com.unam.integrador.model.Servicio servicio = servicioRepository.findById(servicioId)
            .orElseThrow(() -> new IllegalArgumentException("Servicio no encontrado con ID: " + servicioId));
        
        // El método desvincularServicio valida que esté activo
        cliente.desvincularServicio(servicio);
        
        return clienteRepository.save(cliente);
    }
    
    /**
     * Modifica los datos de un cliente existente.
     * 
     * <p>Permite actualizar todos los campos excepto:</p>
     * <ul>
     *   <li>CUIT/DNI (inmutable, es el identificador fiscal único)</li>
     *   <li>Estado de cuenta (gestionado por otra historia de usuario)</li>
     * </ul>
     * 
     * <p>Aplica las mismas validaciones que en el alta del cliente.</p>
     * 
     * @param id el ID del cliente a modificar
     * @param datosActualizados objeto con los nuevos datos del cliente
     * @return el cliente modificado
     * @throws IllegalArgumentException si el cliente no existe o los datos son inválidos
     */
    public CuentaCliente modificarCliente(Long id, CuentaCliente datosActualizados) {
        CuentaCliente cliente = obtenerClientePorId(id);
        
        // Delegar la actualización al modelo rico
        // NO se modifica el estado de cuenta (eso es otra HU)
        cliente.actualizarDatos(
            datosActualizados.getNombre(),
            datosActualizados.getRazonSocial(),
            datosActualizados.getDomicilio(),
            datosActualizados.getEmail(),
            datosActualizados.getTelefono(),
            datosActualizados.getCondicionIva()
        );
        
        return clienteRepository.save(cliente);
    }
    
    /**
     * Cambia el estado de la cuenta de un cliente.
     * 
     * <p>Este método orquesta el cambio de estado, delegando la lógica de negocio
     * al modelo rico y persistiendo los cambios en la base de datos.</p>
     * 
     * <p>Se crea automáticamente un registro de auditoría del cambio en el historial.</p>
     * 
     * @param clienteId el ID del cliente cuyo estado se va a cambiar
     * @param nuevoEstado el nuevo estado para la cuenta
     * @param motivo la justificación del cambio de estado (obligatorio)
     * @return el cliente con el estado actualizado
     * @throws IllegalArgumentException si el cliente no existe, si el nuevo estado es null,
     *                                  si el motivo es inválido, o si el estado ya es el actual
     */
    public CuentaCliente cambiarEstado(Long clienteId, EstadoCuenta nuevoEstado, String motivo) {
        CuentaCliente cliente = obtenerClientePorId(clienteId);
        
        // Delegar la lógica de negocio al modelo rico
        cliente.cambiarEstado(nuevoEstado, motivo);
        
        return clienteRepository.save(cliente);
    }
    
    /**
     * Obtiene el historial completo de cambios de estado de un cliente.
     * 
     * <p>Los cambios se retornan ordenados por fecha descendente (más recientes primero).</p>
     * 
     * @param clienteId el ID del cliente
     * @return lista de cambios de estado ordenados cronológicamente
     * @throws IllegalArgumentException si el cliente no existe
     */
    @Transactional(readOnly = true)
    public List<CambioEstadoCuenta> obtenerHistorialEstados(Long clienteId) {
        // Validar que el cliente existe
        obtenerClientePorId(clienteId);
        
        return cambioEstadoRepository.findByClienteIdOrderByFechaCambioDesc(clienteId);
    }

}