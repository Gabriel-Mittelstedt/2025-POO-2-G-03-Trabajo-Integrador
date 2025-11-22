package com.unam.integrador.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.unam.integrador.model.CambioEstadoCuenta;

/**
 * Repositorio para la gestión de cambios de estado de cuentas de clientes.
 * Proporciona operaciones CRUD y consultas específicas para el historial de estados.
 */
@Repository
public interface CambioEstadoCuentaRepository extends JpaRepository<CambioEstadoCuenta, Long> {
    
    /**
     * Obtiene todos los cambios de estado de un cliente específico,
     * ordenados por fecha de cambio descendente (más recientes primero).
     * 
     * @param clienteId el ID del cliente
     * @return lista de cambios de estado ordenados cronológicamente
     */
    List<CambioEstadoCuenta> findByClienteIdOrderByFechaCambioDesc(Long clienteId);
}
