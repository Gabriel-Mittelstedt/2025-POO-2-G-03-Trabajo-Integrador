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
     * Busca todos los pagos que comparten el mismo número de recibo.
     * Útil para reconstruir recibos consolidados de pagos combinados.
     * 
     * @param numeroRecibo Número de recibo compartido
     * @return Lista de pagos con ese número de recibo
     */
    List<Pago> findByNumeroRecibo(String numeroRecibo);
}
