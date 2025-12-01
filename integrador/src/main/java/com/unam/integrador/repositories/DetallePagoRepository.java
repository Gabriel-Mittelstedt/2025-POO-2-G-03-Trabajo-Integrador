package com.unam.integrador.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.unam.integrador.model.DetallePago;

/**
 * Repositorio para la gestión de detalles de pago.
 * Permite consultar la relación entre pagos y facturas.
 */
@Repository
public interface DetallePagoRepository extends JpaRepository<DetallePago, Long> {
    
    /**
     * Busca todos los detalles asociados a un pago específico.
     * Permite ver a qué facturas se aplicó un pago.
     * 
     * @param pagoId ID del pago
     * @return Lista de detalles de pago
     */
    List<DetallePago> findByPagoIDPago(Long pagoId);
    
    /**
     * Busca todos los detalles asociados a una factura específica.
     * Permite ver qué pagos se aplicaron a una factura.
     * 
     * @param facturaId ID de la factura
     * @return Lista de detalles de pago
     */
    List<DetallePago> findByFacturaIdFactura(Long facturaId);
    
    /**
     * Busca detalles de pago por número de recibo.
     * Útil para reconstruir recibos consolidados de pagos combinados.
     * 
     * @param numeroRecibo Número de recibo compartido
     * @return Lista de detalles de pago con ese número de recibo
     */
    @Query("SELECT dp FROM DetallePago dp WHERE dp.pago.numeroRecibo = :numeroRecibo ORDER BY dp.factura.idFactura")
    List<DetallePago> findByNumeroRecibo(@Param("numeroRecibo") String numeroRecibo);
    
    /**
     * Obtiene todos los detalles de pago de un cliente específico.
     * Útil para reportes de pagos por cliente.
     * 
     * @param clienteId ID del cliente
     * @return Lista de detalles de pago del cliente
     */
    @Query("SELECT dp FROM DetallePago dp WHERE dp.factura.cliente.id = :clienteId ORDER BY dp.fechaAplicacion DESC")
    List<DetallePago> findByClienteId(@Param("clienteId") Long clienteId);
}
