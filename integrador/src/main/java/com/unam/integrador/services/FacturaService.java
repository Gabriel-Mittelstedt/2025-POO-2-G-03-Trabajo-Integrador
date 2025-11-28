package com.unam.integrador.services;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unam.integrador.model.CuentaCliente;
import com.unam.integrador.model.Factura;
import com.unam.integrador.model.ItemFactura;
import com.unam.integrador.model.Servicio;
import com.unam.integrador.model.ServicioContratado;
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
    
    /**
     * Emite una factura individual usando los servicios contratados activos del cliente.
     * Los items se generan automáticamente desde los servicios asignados.
     * @param clienteId ID del cliente
     * @param periodo Período de facturación (formato YYYYMM)
     * @param fechaEmision Fecha de emisión
     * @param fechaVencimiento Fecha de vencimiento
     * @param porcentajeDescuento Descuento opcional (puede ser null)
     * @param motivoDescuento Motivo del descuento (requerido si hay descuento)
     * @return Factura generada
     */
    @Transactional
    public Factura emitirFacturaDesdeServiciosContratados(
            Long clienteId, 
            String periodo, 
            LocalDate fechaEmision,
            LocalDate fechaVencimiento,
            Double porcentajeDescuento,
            String motivoDescuento) {
        
        // 1. Obtener cliente
        CuentaCliente cliente = clienteRepository.findById(clienteId)
            .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado con ID: " + clienteId));
        
        // 2. Obtener servicios contratados activos
        List<ServicioContratado> serviciosContratados = cliente.getServiciosContratadosActivos();
        if (serviciosContratados.isEmpty()) {
            throw new IllegalArgumentException("El cliente no tiene servicios contratados activos");
        }
        
        // 3. Determinar tipo de factura (delegar al dominio)
        TipoFactura tipoFactura = Factura.determinarTipoFactura(
            CONDICION_IVA_EMISOR,
            cliente.getCondicionIva()
        );
        
        // 4. Obtener serie y número
        int serie = obtenerSerie(tipoFactura);
        int numero = obtenerSiguienteNumeroFactura(serie);
        
        // 5. Crear factura
        Factura factura = new Factura(
            serie, 
            numero, 
            cliente,
            fechaEmision, 
            fechaVencimiento, 
            periodo, 
            tipoFactura
        );
        
        // 6. Validar cliente activo (delegar al dominio)
        factura.validarClienteActivo();
        
        // 7. Agregar items desde servicios contratados
        for (ServicioContratado servicioContratado : serviciosContratados) {
            Servicio servicio = servicioContratado.getServicio();
            
            ItemFactura item = new ItemFactura(
                servicio.getNombre(),                       // descripcion
                servicioContratado.getPrecioContratado(),   // precioUnitario (precio específico del contrato)
                1,                                          // cantidad (siempre 1 para servicios mensuales)
                servicio.getAlicuotaIVA()                   // alicuotaIVA
            );
            
            factura.agregarItem(item);
        }
        
        // 8. Aplicar descuento si existe
        if (porcentajeDescuento != null && porcentajeDescuento > 0) {
            if (motivoDescuento == null || motivoDescuento.isBlank()) {
                throw new IllegalArgumentException("El motivo del descuento es obligatorio");
            }
            factura.aplicarDescuento(porcentajeDescuento, motivoDescuento);
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

    /**
     * Lista facturas usando filtros opcionales.
     * Si un parámetro es null o vacío se ignora ese criterio.
     * @param estado Nombre del enum EstadoFactura (ej: PENDIENTE)
     * @param tipo Nombre del enum TipoFactura (ej: A)
     * @param periodo Texto del periodo a buscar (coincidencia parcial)
     * @return Lista filtrada de facturas
     */
    @Transactional(readOnly = true)
    public Iterable<Factura> listarFacturasFiltradas(String estado, String tipo, String periodo) {
        Iterable<Factura> all = facturaRepository.findAll();

        return StreamSupport.stream(all.spliterator(), false)
                .filter(f -> {
                    if (estado == null || estado.isBlank()) return true;
                    try {
                        return f.getEstado() != null && f.getEstado().name().equalsIgnoreCase(estado);
                    } catch (Exception ex) {
                        return true;
                    }
                })
                .filter(f -> {
                    if (tipo == null || tipo.isBlank()) return true;
                    try {
                        return f.getTipo() != null && f.getTipo().name().equalsIgnoreCase(tipo);
                    } catch (Exception ex) {
                        return true;
                    }
                })
                .filter(f -> {
                    if (periodo == null || periodo.isBlank()) return true;
                    return f.getPeriodo() != null && f.getPeriodo().toLowerCase().contains(periodo.toLowerCase());
                })
                .collect(Collectors.toList());
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
