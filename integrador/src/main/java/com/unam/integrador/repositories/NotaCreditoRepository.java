package com.unam.integrador.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.unam.integrador.model.NotaCredito;

/**
 * Repositorio para la gestión de notas de crédito.
 * Proporciona operaciones CRUD y consultas personalizadas.
 */
@Repository
public interface NotaCreditoRepository extends JpaRepository<NotaCredito, Long> {
    
    /**
     * Obtiene la última nota de crédito para una serie específica.
     * Query Method: Spring genera automáticamente la consulta.
     * 
     * @param serie Serie de la nota de crédito
     * @return Última nota de crédito de esa serie ordenada por número descendente, o null si no hay registros
     */
    NotaCredito findFirstBySerieOrderByNroNotaCreditoDesc(int serie);
}
