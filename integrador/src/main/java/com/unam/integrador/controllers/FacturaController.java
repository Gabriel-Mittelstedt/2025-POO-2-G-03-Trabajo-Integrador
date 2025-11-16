package com.unam.integrador.controllers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.unam.integrador.dto.EmitirFacturaRequest;
import com.unam.integrador.dto.FacturaResponse;
import com.unam.integrador.model.Factura;
import com.unam.integrador.model.ItemFactura;
import com.unam.integrador.services.FacturaService;

import jakarta.validation.Valid;

/**
 * Controlador REST para la gestión de facturas.
 * Implementa HU-04: Emisión de factura individual.
 */
@RestController
@RequestMapping("/api/facturas")
@Validated
public class FacturaController {
    
    @Autowired
    private FacturaService facturaService;
    
    /**
     * Emite una factura individual para un cliente.
     * 
     * POST /api/facturas
     * 
     * @param request Datos de la factura a emitir (incluye items)
     * @return Factura emitida con código 201 (Created)
     */
    @PostMapping
    public ResponseEntity<?> emitirFacturaIndividual(@Valid @RequestBody EmitirFacturaRequest request) {
        
        try {
            // Emitir la factura (la lógica está en el servicio y el dominio)
            Factura factura = facturaService.emitirFacturaIndividual(request);
            
            // Convertir a DTO de respuesta
            FacturaResponse response = convertirAFacturaResponse(factura);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            // Error de validación (cliente no encontrado, etc.)
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
            
        } catch (IllegalStateException e) {
            // Error de estado (cliente inactivo, sin servicios, etc.)
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
            
        } catch (Exception e) {
            // Error inesperado
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Error interno del servidor: " + e.getMessage()));
        }
    }
    
    /**
     * Obtiene el detalle completo de una factura por su ID.
     * 
     * GET /api/facturas/{id}
     * 
     * @param id ID de la factura
     * @return Detalle de la factura
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerFactura(@PathVariable Long id) {
        try {
            Factura factura = facturaService.obtenerFacturaPorId(id);
            FacturaResponse response = convertirAFacturaResponse(factura);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Lista todas las facturas o filtra por cliente.
     * 
     * GET /api/facturas?clienteId={id}
     * 
     * @param clienteId ID del cliente (opcional)
     * @return Lista de facturas
     */
    @GetMapping
    public ResponseEntity<List<FacturaResponse>> listarFacturas(
            @RequestParam(required = false) Long clienteId) {
        
        Iterable<Factura> facturas;
        
        if (clienteId != null) {
            facturas = facturaService.listarFacturasPorCliente(clienteId);
        } else {
            facturas = facturaService.listarTodasLasFacturas();
        }
        
        List<FacturaResponse> response = new ArrayList<>();
        for (Factura factura : facturas) {
            response.add(convertirAFacturaResponse(factura));
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Lista facturas por período.
     * 
     * GET /api/facturas/periodo/{periodo}
     * 
     * @param periodo Período de facturación (ej: "2025-11")
     * @return Lista de facturas del período
     */
    @GetMapping("/periodo/{periodo}")
    public ResponseEntity<List<FacturaResponse>> listarFacturasPorPeriodo(@PathVariable String periodo) {
        
        Iterable<Factura> facturas = facturaService.listarFacturasPorPeriodo(periodo);
        
        List<FacturaResponse> response = new ArrayList<>();
        for (Factura factura : facturas) {
            response.add(convertirAFacturaResponse(factura));
        }
        
        return ResponseEntity.ok(response);
    }
    
    // --- Métodos privados auxiliares ---
    
    /**
     * Convierte una entidad Factura a DTO de respuesta.
     */
    private FacturaResponse convertirAFacturaResponse(Factura factura) {
        FacturaResponse response = new FacturaResponse();
        
        // Datos básicos
        response.setIdFactura(factura.getIdFactura());
        response.setSerie(factura.getSerie());
        response.setNroFactura(factura.getNroFactura());
        response.generarNumeroCompleto();
        
        // Cliente
        response.setClienteId(factura.getCliente().getId());
        response.setClienteNombre(factura.getCliente().getNombre());
        response.setClienteCuit(factura.getCliente().getCuitDni());
        
        // Fechas y período
        response.setFechaEmision(factura.getFechaEmision());
        response.setFechaVencimiento(factura.getFechaVencimiento());
        response.setPeriodo(factura.getPeriodo());
        
        // Tipo y estado
        response.setTipo(factura.getTipo());
        response.setEstado(factura.getEstado());
        
        // Montos
        response.setSubtotal(factura.getSubtotal());
        response.setDescuento(factura.getDescuento());
        response.setMotivoDescuento(factura.getMotivoDescuento());
        response.setTotalIva(factura.getTotalIva());
        response.setTotal(factura.getTotal());
        response.setSaldoPendiente(factura.getSaldoPendiente());
        
        // Items
        List<FacturaResponse.ItemFacturaDTO> itemsDTO = new ArrayList<>();
        for (ItemFactura item : factura.getDetalleFactura()) {
            itemsDTO.add(convertirAItemDTO(item));
        }
        response.setItems(itemsDTO);
        
        return response;
    }
    
    /**
     * Convierte un ItemFactura a DTO.
     */
    private FacturaResponse.ItemFacturaDTO convertirAItemDTO(ItemFactura item) {
        FacturaResponse.ItemFacturaDTO dto = new FacturaResponse.ItemFacturaDTO();
        dto.setDescripcion(item.getDescripcion());
        dto.setPrecioUnitario(item.getPrecioUnitario());
        dto.setCantidad(item.getCantidad());
        dto.setAlicuotaIVA(item.getAlicuotaIVA().name());
        dto.setSubtotal(item.getSubtotal());
        dto.setMontoIva(item.getMontoIva());
        dto.setTotal(item.getTotal());
        return dto;
    }
    
    /**
     * Clase interna para respuestas de error.
     */
    private static class ErrorResponse {
        private String mensaje;
        
        public ErrorResponse(String mensaje) {
            this.mensaje = mensaje;
        }
        
        public String getMensaje() {
            return mensaje;
        }
    }
}
