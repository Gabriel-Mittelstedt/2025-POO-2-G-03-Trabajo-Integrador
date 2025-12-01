package com.unam.integrador.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.unam.integrador.model.Factura;
import com.unam.integrador.model.enums.EstadoFactura;
import com.unam.integrador.model.enums.TipoFactura;

/**
 * Repositorio para la gestión de facturas.
 * Proporciona operaciones CRUD y consultas personalizadas.
 */
@Repository
public interface FacturaRepository extends JpaRepository<Factura, Long> {
    
    /**
     * Busca facturas por cliente.
     * @param clienteId ID del cliente
     * @return Lista de facturas del cliente
     */
    List<Factura> findByClienteId(Long clienteId);
    
    /**
     * Busca facturas por estado.
     * @param estado Estado de la factura
     * @return Lista de facturas con ese estado
     */
    List<Factura> findByEstado(EstadoFactura estado);
    
    /**
     * Busca facturas por tipo.
     * @param tipo Tipo de factura (A, B, C)
     * @return Lista de facturas de ese tipo
     */
    List<Factura> findByTipo(TipoFactura tipo);
    
    /**
     * Busca facturas por período.
     * @param periodo Período de facturación como LocalDate
     * @return Lista de facturas de ese período
     */
    List<Factura> findByPeriodo(LocalDate periodo);
    
    /**
     * Busca facturas por cliente y estado.
     * @param clienteId ID del cliente
     * @param estado Estado de la factura
     * @return Lista de facturas
     */
    List<Factura> findByClienteIdAndEstado(Long clienteId, EstadoFactura estado);
    
    /**
     * Obtiene el último número de factura para una serie específica.
     * Query Method: Spring genera automáticamente la consulta.
     * @param serie Serie de la factura
     * @return Última factura de esa serie ordenada por número descendente, o null si no existe
     */
    Factura findFirstBySerieOrderByNroFacturaDesc(int serie);
    
    /**
     * Busca una factura por serie y número.
     * @param serie Serie de la factura
     * @param nroFactura Número de factura
     * @return Factura si existe
     */
    Optional<Factura> findBySerieAndNroFactura(int serie, int nroFactura);
    
    /**
     * Busca facturas impagas (pendientes, vencidas o pagadas parcialmente) de un cliente.
     * Query Method: Spring genera automáticamente la consulta.
     * @param clienteId ID del cliente
     * @param estados Lista de estados considerados como "impagas"
     * @return Lista de facturas impagas del cliente ordenadas por fecha de emisión
     */
    List<Factura> findByClienteIdAndEstadoInOrderByFechaEmisionAsc(Long clienteId, List<EstadoFactura> estados);
    
    /**
     * Verifica si existe una factura no anulada para un cliente en un período específico.
     * Se usa para evitar emitir más de una factura por período al mismo cliente.
     * @param clienteId ID del cliente
     * @param periodo Período de facturación (primer día del mes)
     * @return true si existe una factura no anulada para ese cliente y período
     */
    boolean existsByClienteIdAndPeriodoAndEstadoNot(Long clienteId, LocalDate periodo, EstadoFactura estado);
}
