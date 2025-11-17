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
    List<Pago> findByFacturaIdFactura(Long facturaId);
}
