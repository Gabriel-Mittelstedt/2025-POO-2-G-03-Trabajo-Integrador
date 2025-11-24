package com.unam.integrador.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.unam.integrador.model.CuentaCliente;

/**
 * Repositorio para la gestión de cuentas de clientes.
 * Maneja las operaciones CRUD y consultas específicas.
 */
@Repository
public interface CuentaClienteRepositorie extends JpaRepository<CuentaCliente, Long> {
    
    /**
     * Busca un cliente por su CUIT/DNI
     * @param cuitDni el CUIT o DNI del cliente
     * @return Optional con el cliente si existe
     */
    Optional<CuentaCliente> findByCuitDni(String cuitDni);
    
    /**
     * Verifica si existe un cliente con el CUIT/DNI dado
     * @param cuitDni el CUIT o DNI a verificar
     * @return true si existe, false en caso contrario
     */
    boolean existsByCuitDni(String cuitDni);

    /**
     * Busca clientes cuyo nombre contenga la cadena proporcionada (ignorando mayúsculas)
     * @param nombre fragmento del nombre
     * @return lista de clientes que coinciden
     */
    java.util.List<CuentaCliente> findByNombreContainingIgnoreCase(String nombre);
}
