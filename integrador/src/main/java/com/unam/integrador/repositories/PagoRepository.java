package com.unam.integrador.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.unam.integrador.model.Pago;

/**
 * Repositorio para la gestión de pagos.
 */
@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {
    
    /**
     * Busca pagos por factura.
     * @param facturaId ID de la factura
     * @return Lista de pagos de la factura
     */
    // use PagoFacturaRepository to query pagos by factura

    /**
     * Busca pagos asociados a un número de recibo (útil para recibos que agrupan varios pagos).
     */
    List<Pago> findByNumeroRecibo(String numeroRecibo);

    /**
     * Indica si existe al menos un pago con el número de recibo dado y el método especificado.
     */
    boolean existsByNumeroReciboAndMetodoPago(String numeroRecibo, com.unam.integrador.model.enums.MetodoPago metodoPago);
}
