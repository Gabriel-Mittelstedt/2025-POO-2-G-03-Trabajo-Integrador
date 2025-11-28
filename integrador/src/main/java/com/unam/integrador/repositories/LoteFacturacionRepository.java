package com.unam.integrador.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.unam.integrador.model.LoteFacturacion;

/**
 * Repositorio para la gestión de lotes de facturación masiva.
 */
@Repository
public interface LoteFacturacionRepository extends JpaRepository<LoteFacturacion, Long> {
    
    /**
     * Busca lotes por período
     * @param periodo Período de facturación
     * @return Lista de lotes del período
     */
    List<LoteFacturacion> findByPeriodo(String periodo);
    
    /**
     * Busca lotes no anulados
     * @return Lista de lotes activos
     */
    List<LoteFacturacion> findByAnuladoFalse();
    
    /**
     * Busca lotes anulados
     * @return Lista de lotes anulados
     */
    List<LoteFacturacion> findByAnuladoTrue();
    
    /**
     * Obtiene todos los lotes ordenados por fecha de ejecución descendente
     * @return Lista de lotes ordenados
     */
    List<LoteFacturacion> findAllByOrderByFechaEjecucionDesc();
    
    /**
     * Verifica si existe un lote activo (no anulado) para un período
     * @param periodo Período a verificar
     * @return true si existe un lote activo para el período
     */
    @Query("SELECT COUNT(l) > 0 FROM LoteFacturacion l WHERE l.periodo = :periodo AND l.anulado = false")
    boolean existsLoteActivoPorPeriodo(@Param("periodo") String periodo);
}
