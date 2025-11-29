package com.unam.integrador.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
     * @param serie Serie de la factura
     * @return Último número de factura o 0 si no existe
     */
    @Query("SELECT COALESCE(MAX(f.nroFactura), 0) FROM Factura f WHERE f.serie = :serie")
    int findUltimoNumeroFactura(@Param("serie") int serie);
    
    /**
     * Busca una factura por serie y número.
     * @param serie Serie de la factura
     * @param nroFactura Número de factura
     * @return Factura si existe
     */
    Optional<Factura> findBySerieAndNroFactura(int serie, int nroFactura);
    
    /**
     * Busca facturas impagas (pendientes, vencidas o pagadas parcialmente) de un cliente.
     * Útil para pago combinado de múltiples facturas.
     * @param clienteId ID del cliente
     * @return Lista de facturas impagas del cliente
     */
    @Query("SELECT f FROM Factura f WHERE f.cliente.id = :clienteId AND " +
           "(f.estado = 'PENDIENTE' OR f.estado = 'VENCIDA' OR f.estado = 'PAGADA_PARCIALMENTE') " +
           "ORDER BY f.fechaEmision ASC")
    List<Factura> findFacturasImpagasByCliente(@Param("clienteId") Long clienteId);
}
