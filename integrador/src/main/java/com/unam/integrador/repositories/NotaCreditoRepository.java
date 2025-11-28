package com.unam.integrador.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.unam.integrador.model.NotaCredito;

/**
 * Repositorio para la gestión de notas de crédito.
 * Proporciona operaciones CRUD y consultas personalizadas.
 */
@Repository
public interface NotaCreditoRepository extends JpaRepository<NotaCredito, Long> {
    
    /**
     * Obtiene el último número de nota de crédito para una serie específica.
     * Si no hay notas de crédito previas, devuelve 0.
     * 
     * @param serie Serie de la nota de crédito
     * @return Último número usado o 0 si no hay registros
     */
    @Query("SELECT COALESCE(MAX(nc.nroNotaCredito), 0) FROM NotaCredito nc WHERE nc.serie = :serie")
    int findUltimoNumeroNotaCredito(@Param("serie") int serie);
}
