package com.unam.integrador.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unam.integrador.model.CuentaCliente;
import com.unam.integrador.model.Factura;
import com.unam.integrador.model.ItemFactura;
import com.unam.integrador.model.LoteFacturacion;
import com.unam.integrador.model.NotaCredito;
import com.unam.integrador.model.Servicio;
import com.unam.integrador.model.ServicioContratado;
import com.unam.integrador.model.enums.TipoCondicionIVA;
import com.unam.integrador.model.enums.TipoFactura;
import com.unam.integrador.repositories.CuentaClienteRepositorie;
import com.unam.integrador.repositories.FacturaRepository;
import com.unam.integrador.repositories.LoteFacturacionRepository;
import com.unam.integrador.repositories.NotaCreditoRepository;

/**
 * Servicio para la gestión de facturación masiva.
 * Permite generar facturas para todos los clientes activos con servicios contratados.
 */
@Service
public class FacturacionMasivaService {
    
    @Autowired
    private LoteFacturacionRepository loteRepository;
    
    @Autowired
    private FacturaRepository facturaRepository;
    
    @Autowired
    private CuentaClienteRepositorie clienteRepository;
    
    @Autowired
    private NotaCreditoRepository notaCreditoRepository;
    
    // Configuración del emisor (empresa)
    private static final TipoCondicionIVA CONDICION_IVA_EMISOR = TipoCondicionIVA.RESPONSABLE_INSCRIPTO;
    private static final int SERIE_FACTURA_A = 1;
    private static final int SERIE_FACTURA_B = 2;
    private static final int SERIE_FACTURA_C = 3;
    
    /**
     * Ejecuta la facturación masiva para un período.
     * Genera facturas para todos los clientes activos con servicios contratados.
     * 
     * @param periodo Período de facturación (ej: "Noviembre 2024")
     * @param fechaVencimiento Fecha de vencimiento para todas las facturas
     * @param usuario Usuario que ejecuta la facturación
     * @return Lote de facturación con todas las facturas generadas
     * @throws IllegalArgumentException si ya existe un lote activo para el período
     */
    @Transactional
    public LoteFacturacion ejecutarFacturacionMasiva(String periodo, LocalDate fechaVencimiento, String usuario) {
        // Validar parámetros
        if (periodo == null || periodo.trim().isEmpty()) {
            throw new IllegalArgumentException("El período es obligatorio");
        }
        if (fechaVencimiento == null) {
            throw new IllegalArgumentException("La fecha de vencimiento es obligatoria");
        }
        if (fechaVencimiento.isBefore(LocalDate.now()) || fechaVencimiento.isEqual(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de vencimiento debe ser posterior a hoy");
        }
        if (usuario == null || usuario.trim().isEmpty()) {
            throw new IllegalArgumentException("El usuario es obligatorio");
        }
        
        // Verificar si ya existe un lote activo para el período
        if (loteRepository.existsLoteActivoPorPeriodo(periodo)) {
            throw new IllegalArgumentException(
                "Ya existe una facturación masiva activa para el período: " + periodo + 
                ". Debe anular el lote existente antes de crear uno nuevo."
            );
        }
        
        // Crear el lote de facturación
        LoteFacturacion lote = new LoteFacturacion(periodo, fechaVencimiento, usuario);
        
        // Obtener todos los clientes activos con servicios contratados
        List<CuentaCliente> clientes = clienteRepository.findAll();
        LocalDate fechaEmision = LocalDate.now();
        
        int facturasGeneradas = 0;
        
        for (CuentaCliente cliente : clientes) {
            // Verificar si el cliente puede facturar
            if (!cliente.puedeFacturar()) {
                continue; // Saltar clientes no activos
            }
            
            // Obtener servicios contratados activos
            List<ServicioContratado> serviciosContratados = cliente.getServiciosContratadosActivos();
            if (serviciosContratados.isEmpty()) {
                continue; // Saltar clientes sin servicios
            }
            
            try {
                // Generar factura para este cliente
                Factura factura = generarFacturaParaCliente(
                    cliente, 
                    serviciosContratados, 
                    periodo, 
                    fechaEmision, 
                    fechaVencimiento
                );
                
                // Agregar factura al lote
                lote.agregarFactura(factura);
                facturasGeneradas++;
                
            } catch (Exception e) {
                // Log del error pero continuar con los demás clientes
                System.err.println("Error al generar factura para cliente " + cliente.getId() + ": " + e.getMessage());
            }
        }
        
        if (facturasGeneradas == 0) {
            throw new IllegalStateException(
                "No se generó ninguna factura. Verifique que existan clientes activos con servicios contratados."
            );
        }
        
        // Guardar el lote con todas las facturas
        return loteRepository.save(lote);
    }
    
    /**
     * Genera una factura individual para un cliente con sus servicios contratados.
     */
    private Factura generarFacturaParaCliente(
            CuentaCliente cliente,
            List<ServicioContratado> serviciosContratados,
            String periodo,
            LocalDate fechaEmision,
            LocalDate fechaVencimiento) {
        
        // Determinar tipo de factura según condiciones fiscales
        TipoFactura tipoFactura = Factura.determinarTipoFactura(
            CONDICION_IVA_EMISOR,
            cliente.getCondicionIva()
        );
        
        // Obtener serie y número
        int serie = obtenerSerie(tipoFactura);
        int numero = obtenerSiguienteNumeroFactura(serie);
        
        // Crear la factura
        Factura factura = new Factura(
            serie,
            numero,
            cliente,
            fechaEmision,
            fechaVencimiento,
            periodo,
            tipoFactura
        );
        
        // Agregar items desde servicios contratados
        for (ServicioContratado servicioContratado : serviciosContratados) {
            Servicio servicio = servicioContratado.getServicio();
            
            ItemFactura item = new ItemFactura(
                servicio.getNombre(),
                servicioContratado.getPrecioContratado(),
                1, // Cantidad siempre 1 para servicios mensuales
                servicio.getAlicuotaIVA()
            );
            
            factura.agregarItem(item);
        }
        
        // Guardar la factura
        return facturaRepository.save(factura);
    }
    
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
    
    /**
     * Obtiene todos los lotes de facturación ordenados por fecha de ejecución.
     * @return Lista de lotes ordenados
     */
    @Transactional(readOnly = true)
    public List<LoteFacturacion> obtenerTodosLosLotes() {
        return loteRepository.findAllByOrderByFechaEjecucionDesc();
    }
    
    /**
     * Obtiene un lote por su ID.
     * @param id ID del lote
     * @return Lote encontrado
     * @throws IllegalArgumentException si el lote no existe
     */
    @Transactional(readOnly = true)
    public LoteFacturacion obtenerLotePorId(Long id) {
        return loteRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Lote de facturación no encontrado con ID: " + id));
    }
    
    /**
     * Anula un lote de facturación completo.
     * Genera notas de crédito para todas las facturas del lote.
     * 
     * @param loteId ID del lote a anular
     * @param motivo Motivo de la anulación
     * @param usuario Usuario que realiza la anulación
     * @return Lote anulado
     * @throws IllegalArgumentException si el lote no existe
     * @throws IllegalStateException si el lote no puede ser anulado
     */
    @Transactional
    public LoteFacturacion anularLote(Long loteId, String motivo, String usuario) {
        LoteFacturacion lote = obtenerLotePorId(loteId);
        
        // Validar que el lote puede ser anulado
        if (!lote.puedeSerAnulado()) {
            long facturasConPagos = lote.getCantidadFacturasConPagos();
            throw new IllegalStateException(
                "No se puede anular el lote. " +
                (lote.isAnulado() ? "El lote ya está anulado." : 
                 facturasConPagos + " factura(s) tienen pagos registrados y no pueden anularse.")
            );
        }
        
        // Anular cada factura del lote generando notas de crédito
        for (Factura factura : lote.getFacturas()) {
            if (factura.puedeSerAnulada()) {
                // Generar nota de crédito
                int serieNotaCredito = factura.getSerie();
                int nroNotaCredito = obtenerSiguienteNumeroNotaCredito(serieNotaCredito);
                
                NotaCredito notaCredito = new NotaCredito(
                    serieNotaCredito,
                    nroNotaCredito,
                    LocalDate.now(),
                    factura.getTotal(),
                    "Anulación por lote de facturación masiva. " + motivo,
                    factura.getTipo(),
                    factura
                );
                
                factura.agregarNotaCredito(notaCredito);
                factura.anular();
                
                notaCreditoRepository.save(notaCredito);
                facturaRepository.save(factura);
            }
        }
        
        // Marcar el lote como anulado
        lote.anular(motivo, usuario);
        
        return loteRepository.save(lote);
    }
    
    /**
     * Obtiene el siguiente número de nota de crédito para una serie.
     */
    private int obtenerSiguienteNumeroNotaCredito(int serie) {
        int ultimoNumero = notaCreditoRepository.findUltimoNumeroNotaCredito(serie);
        return ultimoNumero + 1;
    }
    
    /**
     * Obtiene estadísticas resumidas de facturación masiva.
     * @return Arreglo con [totalLotes, lotesActivos, lotesAnulados, montoTotalFacturado]
     */
    @Transactional(readOnly = true)
    public Object[] obtenerEstadisticas() {
        List<LoteFacturacion> todos = loteRepository.findAll();
        
        long totalLotes = todos.size();
        long lotesActivos = todos.stream().filter(l -> !l.isAnulado()).count();
        long lotesAnulados = todos.stream().filter(LoteFacturacion::isAnulado).count();
        BigDecimal montoTotal = todos.stream()
            .filter(l -> !l.isAnulado())
            .map(LoteFacturacion::getMontoTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return new Object[]{totalLotes, lotesActivos, lotesAnulados, montoTotal};
    }
}
