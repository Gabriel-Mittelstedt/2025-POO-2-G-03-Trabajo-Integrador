package com.unam.integrador.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unam.integrador.dto.ReciboDTO;
import com.unam.integrador.model.DetallePago;
import com.unam.integrador.model.Factura;
import com.unam.integrador.model.Pago;
import com.unam.integrador.repositories.DetallePagoRepository;
import com.unam.integrador.repositories.PagoRepository;

/**
 * Servicio para la generación dinámica de comprobantes de recibo.
 * 
 * Este servicio NO persiste Recibos como entidades, sino que genera
 * DTOs (ReciboDTO) a partir de los datos de Pago.
 * 
 * Ventajas de este enfoque:
 * - Elimina redundancia de datos (no duplica información de Pago)
 * - Simplifica el modelo de dominio
 * - Facilita cambios en el formato del recibo sin migraciones de BD
 * - El recibo es un documento generado, no una entidad de negocio
 */
@Service
public class ReciboService {
    
    @Autowired
    private PagoRepository pagoRepository;
    
    @Autowired
    private DetallePagoRepository detallePagoRepository;
    
    /**
     * Genera un ReciboDTO a partir de un Pago individual.
     * 
     * @param pago El pago desde el cual generar el recibo
     * @return ReciboDTO con todos los datos del comprobante
     * @throws IllegalArgumentException si el pago es nulo o inválido
     */
    @Transactional(readOnly = true)
    public ReciboDTO generarReciboDesdePago(Pago pago) {
        if (pago == null) {
            throw new IllegalArgumentException("El pago no puede ser nulo");
        }
        
        // Obtener los detalles de pago asociados
        List<DetallePago> detalles = detallePagoRepository.findByPagoIDPago(pago.getIDPago());
        
        if (detalles.isEmpty()) {
            throw new IllegalStateException("El pago no tiene facturas asociadas");
        }
        
        // Obtener información de las facturas desde los detalles
        StringBuilder facturasInfo = new StringBuilder();
        List<Long> facturasIds = new ArrayList<>();
        String clienteNombre = "";
        String clienteCuitDni = "";
        Long clienteId = null;
        
        for (int i = 0; i < detalles.size(); i++) {
            DetallePago detalle = detalles.get(i);
            Factura factura = detalle.getFactura();
            
            if (i > 0) {
                facturasInfo.append(", ");
            }
            
            facturasInfo.append(String.format("Factura %d-%08d ($%s)", 
                factura.getSerie(), 
                factura.getNroFactura(), 
                detalle.getMontoAplicado())); // Usar el monto aplicado del detalle
            facturasIds.add(factura.getIdFactura());
            
            if (factura.getCliente() != null) {
                clienteNombre = factura.getCliente().getNombre();
                clienteCuitDni = factura.getCliente().getCuitDni();
                clienteId = factura.getCliente().getId();
            }
        }
        
        // Generar número de recibo (usar numeroRecibo del pago o generar uno)
        String numeroRecibo = pago.getNumeroRecibo() != null 
            ? pago.getNumeroRecibo() 
            : generarNumeroRecibo(pago.getIDPago());
        
        // Generar desglose de pagos (un pago puede tener múltiples detalles aplicados a diferentes facturas)
        List<ReciboDTO.DetallePagoDTO> desglosePagos = new ArrayList<>();
        for (DetallePago detalle : detalles) {
            Factura factura = detalle.getFactura();
            String numeroFactura = String.format("%d-%08d", factura.getSerie(), factura.getNroFactura());
            
            desglosePagos.add(ReciboDTO.DetallePagoDTO.builder()
                .metodoPago(pago.getMetodoPago())
                .numeroFactura(numeroFactura)
                .facturaId(factura.getIdFactura())
                .monto(detalle.getMontoAplicado())
                .build());
        }
        
        return ReciboDTO.builder()
            .numero(numeroRecibo)
            .fecha(pago.getFechaPago())
            .monto(pago.getMonto())
            .metodoPago(pago.getMetodoPago())
            .referencia(pago.getReferencia())
            .facturasAsociadas(facturasInfo.toString())
            .facturasIds(facturasIds)
            .clienteNombre(clienteNombre)
            .clienteCuitDni(clienteCuitDni)
            .clienteId(clienteId)
            .pagoId(pago.getIDPago())
            .observaciones(null)
            .desglosePagos(desglosePagos)
            .build();
    }
    
    /**
     * Genera un ReciboDTO a partir de un ID de pago.
     * 
     * @param pagoId ID del pago
     * @return ReciboDTO generado
     * @throws IllegalArgumentException si no se encuentra el pago
     */
    @Transactional(readOnly = true)
    public ReciboDTO generarReciboPorPagoId(Long pagoId) {
        Pago pago = pagoRepository.findById(pagoId)
            .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado con ID: " + pagoId));
        
        return generarReciboDesdePago(pago);
    }
    
    /**
     * Genera un ReciboDTO consolidado para múltiples pagos que comparten
     * el mismo número de recibo (pagos combinados).
     * 
     * @param numeroRecibo Número de recibo compartido
     * @return ReciboDTO con información consolidada de todos los pagos
     * @throws IllegalArgumentException si no se encuentran pagos
     */
    @Transactional(readOnly = true)
    public ReciboDTO generarReciboConsolidado(String numeroRecibo) {
        if (numeroRecibo == null || numeroRecibo.trim().isEmpty()) {
            throw new IllegalArgumentException("El número de recibo no puede estar vacío");
        }
        
        // Buscar todos los pagos con ese número de recibo
        List<Pago> pagos = pagoRepository.findByNumeroRecibo(numeroRecibo);
        
        if (pagos.isEmpty()) {
            throw new IllegalArgumentException("No se encontraron pagos con el número de recibo: " + numeroRecibo);
        }
        
        return generarReciboDesdeMultiplesPagos(pagos, numeroRecibo);
    }
    
    /**
     * Genera un ReciboDTO consolidado desde una lista de pagos.
     * Útil para pagos combinados que afectan múltiples facturas.
     * 
     * @param pagos Lista de pagos asociados al mismo recibo
     * @param numeroRecibo Número de recibo consolidado
     * @return ReciboDTO con información agregada
     */
    @Transactional(readOnly = true)
    public ReciboDTO generarReciboDesdeMultiplesPagos(List<Pago> pagos, String numeroRecibo) {
        if (pagos == null || pagos.isEmpty()) {
            throw new IllegalArgumentException("La lista de pagos no puede estar vacía");
        }
        
        // Consolidar información de todos los pagos
        BigDecimal montoTotal = pagos.stream()
            .map(Pago::getMonto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Tomar la fecha del primer pago
        LocalDate fecha = pagos.get(0).getFechaPago();
        
        // Tomar el método de pago del primer pago (pueden ser mixtos)
        // En caso de pago combinado, se muestra el método principal
        var metodoPago = pagos.get(0).getMetodoPago();
        
        // Consolidar referencias
        String referencia = pagos.stream()
            .map(Pago::getReferencia)
            .filter(r -> r != null && !r.trim().isEmpty())
            .collect(Collectors.joining("; "));
        
        // Consolidar facturas asociadas
        List<Long> facturasIds = new ArrayList<>();
        StringBuilder facturasInfo = new StringBuilder();
        String clienteNombre = "";
        String clienteCuitDni = "";
        Long clienteId = null;
        
        for (Pago pago : pagos) {
            List<DetallePago> detalles = detallePagoRepository.findByPagoIDPago(pago.getIDPago());
            
            for (DetallePago detalle : detalles) {
                Factura factura = detalle.getFactura();
                
                if (facturasInfo.length() > 0) {
                    facturasInfo.append(", ");
                }
                facturasInfo.append(String.format("Factura %d-%08d ($%s)", 
                    factura.getSerie(), 
                    factura.getNroFactura(), 
                    detalle.getMontoAplicado())); // Usar el monto aplicado del detalle
                
                if (!facturasIds.contains(factura.getIdFactura())) {
                    facturasIds.add(factura.getIdFactura());
                }
                
                // Tomar datos del cliente de la primera factura
                if (clienteNombre.isEmpty() && factura.getCliente() != null) {
                    clienteNombre = factura.getCliente().getNombre();
                    clienteCuitDni = factura.getCliente().getCuitDni();
                    clienteId = factura.getCliente().getId();
                }
            }
        }
        
        // Agregar observaciones si hay pagos con método SALDO_A_FAVOR
        StringBuilder observaciones = new StringBuilder();
        BigDecimal saldoAFavorAplicado = pagos.stream()
            .filter(p -> p.getMetodoPago() != null && 
                        p.getMetodoPago().name().equals("SALDO_A_FAVOR"))
            .map(Pago::getMonto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (saldoAFavorAplicado.compareTo(BigDecimal.ZERO) > 0) {
            observaciones.append(String.format("Saldo a favor aplicado: $%s", saldoAFavorAplicado));
        }
        
        // Generar desglose de pagos consolidado
        List<ReciboDTO.DetallePagoDTO> desglosePagos = new ArrayList<>();
        for (Pago pago : pagos) {
            List<DetallePago> detalles = detallePagoRepository.findByPagoIDPago(pago.getIDPago());
            
            for (DetallePago detalle : detalles) {
                Factura factura = detalle.getFactura();
                String numeroFactura = String.format("%d-%08d", factura.getSerie(), factura.getNroFactura());
                
                desglosePagos.add(ReciboDTO.DetallePagoDTO.builder()
                    .metodoPago(pago.getMetodoPago())
                    .numeroFactura(numeroFactura)
                    .facturaId(factura.getIdFactura())
                    .monto(detalle.getMontoAplicado())
                    .build());
            }
        }
        
        return ReciboDTO.builder()
            .numero(numeroRecibo)
            .fecha(fecha)
            .monto(montoTotal)
            .metodoPago(metodoPago)
            .referencia(referencia)
            .facturasAsociadas(facturasInfo.toString())
            .facturasIds(facturasIds)
            .clienteNombre(clienteNombre)
            .clienteCuitDni(clienteCuitDni)
            .clienteId(clienteId)
            .pagoId(pagos.get(0).getIDPago()) // ID del primer pago para referencia
            .observaciones(observaciones.length() > 0 ? observaciones.toString() : null)
            .desglosePagos(desglosePagos)
            .build();
    }
    
    /**
     * Genera un número de recibo basado en el ID del pago.
     * Formato: 8 dígitos con ceros a la izquierda.
     * 
     * @param pagoId ID del pago
     * @return Número de recibo formateado
     */
    private String generarNumeroRecibo(Long pagoId) {
        if (pagoId == null) {
            throw new IllegalArgumentException("El ID del pago no puede ser nulo");
        }
        return String.format("%08d", pagoId);
    }
    
    /**
     * Lista todos los recibos (generados dinámicamente desde los pagos).
     * 
     * @return Lista de ReciboDTO
     */
    @Transactional(readOnly = true)
    public List<ReciboDTO> listarTodosLosRecibos() {
        List<Pago> pagos = pagoRepository.findAll();
        
        return pagos.stream()
            .map(this::generarReciboDesdePago)
            .collect(Collectors.toList());
    }
    
    /**
     * Busca recibos por nombre de cliente.
     * 
     * @param clienteNombre Nombre del cliente (búsqueda parcial)
     * @return Lista de ReciboDTO que coinciden
     */
    @Transactional(readOnly = true)
    public List<ReciboDTO> buscarRecibosPorCliente(String clienteNombre) {
        if (clienteNombre == null || clienteNombre.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        List<Pago> pagos = pagoRepository.findAll();
        
        return pagos.stream()
            .filter(pago -> {
                // Obtener los detalles de pago para verificar si alguna factura pertenece al cliente
                List<DetallePago> detalles = detallePagoRepository.findByPagoIDPago(pago.getIDPago());
                
                return detalles.stream().anyMatch(detalle -> {
                    Factura factura = detalle.getFactura();
                    if (factura == null || factura.getCliente() == null) {
                        return false;
                    }
                    String nombre = factura.getCliente().getNombre();
                    return nombre != null && nombre.toLowerCase().contains(clienteNombre.toLowerCase());
                });
            })
            .map(this::generarReciboDesdePago)
            .collect(Collectors.toList());
    }
}
