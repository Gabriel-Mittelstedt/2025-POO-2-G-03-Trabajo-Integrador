package com.unam.integrador.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.unam.integrador.model.PagoFactura;

@Repository
public interface PagoFacturaRepository extends JpaRepository<PagoFactura, Long> {
    @Query("SELECT pf FROM PagoFactura pf WHERE pf.factura.idFactura = :facturaId")
    List<PagoFactura> findByFacturaId(@Param("facturaId") Long facturaId);

    @Query("SELECT pf FROM PagoFactura pf WHERE pf.pago.IDPago = :pagoId")
    List<PagoFactura> findByPagoId(@Param("pagoId") Long pagoId);
}
