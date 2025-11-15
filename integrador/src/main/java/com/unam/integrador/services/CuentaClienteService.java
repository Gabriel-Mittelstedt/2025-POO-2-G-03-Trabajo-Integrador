
package com.unam.integrador.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unam.integrador.model.CuentaCliente;
import com.unam.integrador.repositories.CuentaClienteRepositorie;

/**
 * Servicio para la gestión de cuentas de clientes.
 * Maneja las operaciones CRUD y lógica de negocio.
 */
@Service
@Transactional
public class CuentaClienteService {
    
    @Autowired
    private CuentaClienteRepositorie clienteRepository;
    
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
    

}