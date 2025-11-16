package com.unam.integrador.services;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unam.integrador.dto.EmitirFacturaRequest;
import com.unam.integrador.dto.ItemFacturaRequest;
import com.unam.integrador.model.CuentaCliente;
import com.unam.integrador.model.Factura;
import com.unam.integrador.model.ItemFactura;
import com.unam.integrador.model.enums.EstadoCuenta;
import com.unam.integrador.model.enums.TipoCondicionIVA;
import com.unam.integrador.model.enums.TipoFactura;
import com.unam.integrador.repositories.CuentaClienteRepositorie;
import com.unam.integrador.repositories.FacturaRepository;

/**
 * Servicio de aplicación para la gestión de facturas.
 * 
 * Este servicio actúa como orquestador delgado, coordinando las operaciones
 * pero delegando la lógica de negocio a las entidades del dominio (modelo RICO).
 */
@Service
public class FacturaService {
    
    @Autowired
    private FacturaRepository facturaRepository;
    
    @Autowired
    private CuentaClienteRepositorie clienteRepository;
    
    // Configuración para el emisor (empresa)
    // TODO: En producción esto debería venir de configuración o base de datos
    private static final TipoCondicionIVA CONDICION_IVA_EMISOR = TipoCondicionIVA.RESPONSABLE_INSCRIPTO;
    private static final int SERIE_FACTURA_A = 1;
    private static final int SERIE_FACTURA_B = 2;
    private static final int SERIE_FACTURA_C = 3;
    private static final int DIAS_VENCIMIENTO = 10;
    
    /**
     * Emite una factura individual para un cliente.
     * 
     * La lógica de negocio está en las entidades Factura e ItemFactura,
     * este método solo coordina el proceso.
     * 
     * @param request Datos de la factura a emitir (incluye items)
     * @return Factura generada
     * @throws IllegalArgumentException si los parámetros son inválidos
     * @throws IllegalStateException si el cliente no está activo
     */
    @Transactional
    public Factura emitirFacturaIndividual(EmitirFacturaRequest request) {
        
        // 1. Obtener y validar cliente
        CuentaCliente cliente = clienteRepository.findById(request.getClienteId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Cliente no encontrado con ID: " + request.getClienteId()));
        
        // Validar que el cliente esté activo
        if (cliente.getEstado() != EstadoCuenta.ACTIVA) {
            throw new IllegalStateException(
                "No se puede emitir factura. El cliente no tiene cuenta activa. Estado actual: " 
                + cliente.getEstado().getDescripcion()
            );
        }
        
        // 2. Validar que haya items
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException(
                "No se puede emitir factura sin items."
            );
        }
        
        // 3. Determinar tipo de factura (delegar al dominio)
        TipoFactura tipoFactura = Factura.determinarTipoFactura(
            CONDICION_IVA_EMISOR,
            cliente.getCondicionIva()
        );
        
        // 4. Obtener serie y número de factura
        int serie = obtenerSerie(tipoFactura);
        int numeroFactura = obtenerSiguienteNumeroFactura(serie);
        
        // 5. Calcular fecha de vencimiento
        LocalDate fechaVencimiento = request.getFechaEmision().plusDays(DIAS_VENCIMIENTO);
        
        // 6. Crear la factura
        Factura factura = new Factura(
            serie,
            numeroFactura,
            cliente,
            request.getFechaEmision(),
            fechaVencimiento,
            request.getPeriodo(),
            tipoFactura
        );
        
        // 7. Agregar items desde el request (delegar al dominio)
        for (ItemFacturaRequest itemRequest : request.getItems()) {
            ItemFactura item = new ItemFactura(
                itemRequest.getDescripcion(),
                itemRequest.getPrecioUnitario(),
                itemRequest.getCantidad(),
                itemRequest.getAlicuotaIVA()
            );
            
            // Agregar item a la factura (esto calcula automáticamente)
            factura.agregarItem(item);
        }
        
        // 8. Aplicar descuento si corresponde (delegar al dominio)
        if (request.getPorcentajeDescuento() != null && request.getPorcentajeDescuento() > 0) {
            if (request.getMotivoDescuento() == null || request.getMotivoDescuento().trim().isEmpty()) {
                throw new IllegalArgumentException(
                    "El motivo del descuento es obligatorio cuando se aplica un descuento"
                );
            }
            factura.aplicarDescuento(request.getPorcentajeDescuento(), request.getMotivoDescuento());
        }
        
        // 9. Persistir factura
        return facturaRepository.save(factura);
    }
    
    /**
     * Busca una factura por su ID.
     * @param id ID de la factura
     * @return Factura encontrada
     * @throws IllegalArgumentException si no existe
     */
    @Transactional(readOnly = true)
    public Factura obtenerFacturaPorId(Long id) {
        return facturaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada con ID: " + id));
    }
    
    /**
     * Lista todas las facturas.
     * @return Lista de todas las facturas
     */
    @Transactional(readOnly = true)
    public Iterable<Factura> listarTodasLasFacturas() {
        return facturaRepository.findAll();
    }
    
    /**
     * Lista facturas por cliente.
     * @param clienteId ID del cliente
     * @return Lista de facturas
     */
    @Transactional(readOnly = true)
    public Iterable<Factura> listarFacturasPorCliente(Long clienteId) {
        return facturaRepository.findByClienteId(clienteId);
    }
    
    /**
     * Lista facturas por período.
     * @param periodo Período de facturación
     * @return Lista de facturas
     */
    @Transactional(readOnly = true)
    public Iterable<Factura> listarFacturasPorPeriodo(String periodo) {
        return facturaRepository.findByPeriodo(periodo);
    }
    
    // --- Métodos privados auxiliares ---
    
    /**
     * Obtiene la serie según el tipo de factura.
     */
    private int obtenerSerie(TipoFactura tipo) {
        return switch (tipo) {
            case A -> SERIE_FACTURA_A;
            case B -> SERIE_FACTURA_B;
            case C -> SERIE_FACTURA_C;
        };
    }
    
    /**
     * Obtiene el siguiente número de factura para una serie.
     */
    private int obtenerSiguienteNumeroFactura(int serie) {
        int ultimoNumero = facturaRepository.findUltimoNumeroFactura(serie);
        return ultimoNumero + 1;
    }
}
