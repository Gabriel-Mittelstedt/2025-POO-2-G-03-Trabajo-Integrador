package com.unam.integrador.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.unam.integrador.model.LoteFacturacion;

/**
 * Repositorio para la gestión de lotes de facturación masiva.
 * Proporciona operaciones CRUD y consultas personalizadas.
 */
@Repository
public interface LoteFacturacionRepository extends JpaRepository<LoteFacturacion, Long> {
    
    /**
     * Busca un lote por el período de facturación (fecha).
     * 
     * @param periodoFecha Fecha del período (primer día del mes)
     * @return Lote si existe
     */
    Optional<LoteFacturacion> findByPeriodoFecha(LocalDate periodoFecha);
    
    /**
     * Busca todos los lotes que no están anulados.
     * 
     * @return Lista de lotes activos
     */
    List<LoteFacturacion> findByAnuladoFalse();
    
    /**
     * Busca todos los lotes ordenados por fecha de ejecución descendente.
     * 
     * @return Lista de lotes ordenados
     */
    List<LoteFacturacion> findAllByOrderByFechaEjecucionDesc();
    
    /**
     * Verifica si existe un lote no anulado para un período específico.
     * 
     * @param periodoFecha Fecha del período
     * @return true si existe un lote activo para ese período
     */
    boolean existsByPeriodoFechaAndAnuladoFalse(LocalDate periodoFecha);
}
