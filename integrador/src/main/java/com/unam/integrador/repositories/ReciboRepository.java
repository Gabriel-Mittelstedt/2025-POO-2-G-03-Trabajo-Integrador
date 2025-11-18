package com.unam.integrador.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.unam.integrador.model.Recibo;

/**
 * Repositorio para la gestión de recibos.
 */
@Repository
public interface ReciboRepository extends JpaRepository<Recibo, Long> {
    
    /**
     * Obtiene el último número de recibo generado.
     * @return Último número o 0 si no existe
     */
    @Query("SELECT COALESCE(MAX(CAST(r.numero AS int)), 0) FROM Recibo r")
    int findUltimoNumeroRecibo();
}
