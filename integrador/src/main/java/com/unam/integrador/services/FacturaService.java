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
import com.unam.integrador.model.NotaCredito;
import com.unam.integrador.model.PeriodoFacturacion;
import com.unam.integrador.model.Servicio;
import com.unam.integrador.model.ServicioContratado;
import com.unam.integrador.model.enums.EstadoFactura;
import com.unam.integrador.model.enums.TipoCondicionIVA;
import com.unam.integrador.model.enums.TipoFactura;
import com.unam.integrador.repositories.CuentaClienteRepositorie;
import com.unam.integrador.repositories.FacturaRepository;
import com.unam.integrador.repositories.NotaCreditoRepository;

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
    
    @Autowired
    private NotaCreditoRepository notaCreditoRepository;
    
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
     * @param periodo Período de facturación como LocalDate (se usará el primer día del mes)
     * @param fechaEmision Fecha de emisión
     * @param fechaVencimiento Fecha de vencimiento
     * @param porcentajeDescuento Descuento opcional (puede ser null)
     * @param motivoDescuento Motivo del descuento (requerido si hay descuento)
     * @return Factura generada
     */
    @Transactional
    public Factura emitirFacturaDesdeServiciosContratados(
            Long clienteId, 
            LocalDate periodo, 
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
        
        // 3. Validar que no exista otra factura no anulada para el mismo período
        LocalDate periodoNormalizado = periodo.withDayOfMonth(1);
        if (facturaRepository.existsByClienteIdAndPeriodoAndEstadoNot(clienteId, periodoNormalizado, EstadoFactura.ANULADA)) {
            throw new IllegalStateException(
                "Ya existe una factura emitida para este cliente en el período seleccionado. " +
                "Solo se puede emitir una nueva factura si la anterior fue anulada."
            );
        }
        
        // 4. Determinar tipo de factura (delegar al dominio)
        TipoFactura tipoFactura = Factura.determinarTipoFactura(
            CONDICION_IVA_EMISOR,
            cliente.getCondicionIva()
        );
        
        // 5. Obtener serie y número
        int serie = obtenerSerie(tipoFactura);
        int numero = obtenerSiguienteNumeroFactura(serie);
        
        // 6. Crear factura
        Factura factura = new Factura(
            serie, 
            numero, 
            cliente,
            fechaEmision, 
            fechaVencimiento, 
            periodo, 
            tipoFactura
        );
        
        // 7. Validar fechas (delegar al dominio)
        factura.validarFechas();
        
        // 8. Validar cliente activo (delegar al dominio)
        factura.validarClienteActivo();
        
        // 9. Agregar items desde servicios contratados
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
        
        // 10. Aplicar descuento si existee
        if (porcentajeDescuento != null && porcentajeDescuento > 0) {
            if (motivoDescuento == null || motivoDescuento.isBlank()) {
                throw new IllegalArgumentException("El motivo del descuento es obligatorio");
            }
            factura.aplicarDescuento(porcentajeDescuento, motivoDescuento);
        }
        
        // 11. Persistir factura
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
     * @param periodo Período de facturación como LocalDate
     * @return Lista de facturas
     */
    @Transactional(readOnly = true)
    public Iterable<Factura> listarFacturasPorPeriodo(LocalDate periodo) {
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
                    String periodoFormateado = f.getPeriodoFormateado();
                    return periodoFormateado != null && periodoFormateado.toLowerCase().contains(periodo.toLowerCase());
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

    /**
     * Anula una factura individual generando una nota de crédito total.
     * Solo se pueden anular facturas no pagadas o con saldo completo.
     * 
     * @param facturaId ID de la factura a anular
     * @param motivo Motivo de la anulación
     * @return Factura anulada con la nota de crédito asociada
     * @throws IllegalArgumentException si la factura no existe
     * @throws IllegalStateException si la factura no puede ser anulada
     */
    @Transactional
    public Factura anularFactura(Long facturaId, String motivo) {
        // Validar que el motivo no esté vacío
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IllegalArgumentException("Debe ingresar un motivo para la anulación");
        }

        // Buscar la factura
        Factura factura = facturaRepository.findById(facturaId)
            .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada"));

        // Validar que puede ser anulada (delega en el modelo)
        if (!factura.puedeSerAnulada()) {
            throw new IllegalStateException(
                "No se puede anular la factura. Estado actual: " + factura.getEstado().getDescripcion() +
                ". Solo se pueden anular facturas sin pagos o con saldo completo."
            );
        }

        // Generar nota de crédito total
        int serieNotaCredito = factura.getSerie(); // La nota de crédito usa la misma serie
        int nroNotaCredito = obtenerSiguienteNumeroNotaCredito(serieNotaCredito);

        NotaCredito notaCredito = new NotaCredito(
            serieNotaCredito,
            nroNotaCredito,
            LocalDate.now(),
            factura.getTotal(), // Monto total de la factura
            motivo,
            factura.getTipo(),
            factura
        );

        // Agregar la nota de crédito a la factura
        factura.agregarNotaCredito(notaCredito);

        // Anular la factura (cambia el estado)
        factura.anular();

        // Persistir cambios
        notaCreditoRepository.save(notaCredito);
        facturaRepository.save(factura);

        return factura;
    }

    /**
     * Obtiene el siguiente número de nota de crédito para una serie.
     */
    private int obtenerSiguienteNumeroNotaCredito(int serie) {
        int ultimoNumero = notaCreditoRepository.findUltimoNumeroNotaCredito(serie);
        return ultimoNumero + 1;
    }

    /**
     * Emite una factura proporcional para un cliente en un rango de fechas específico.
     * Calcula automáticamente el monto proporcional basado en los días efectivos del período.
     * 
     * @param clienteId ID del cliente
     * @param inicioPeriodo Fecha de inicio del período a facturar
     * @param finPeriodo Fecha de fin del período a facturar
     * @param fechaEmision Fecha de emisión de la factura
     * @param fechaVencimiento Fecha de vencimiento de la factura
     * @param porcentajeDescuento Descuento opcional (puede ser null)
     * @param motivoDescuento Motivo del descuento (requerido si hay descuento)
     * @return Factura proporcional generada
     */
    @Transactional
    public Factura emitirFacturaProporcional(
            Long clienteId,
            LocalDate inicioPeriodo,
            LocalDate finPeriodo,
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
        
        // 3. Validar que no exista otra factura no anulada para el mismo período
        LocalDate periodoNormalizado = inicioPeriodo.withDayOfMonth(1);
        if (facturaRepository.existsByClienteIdAndPeriodoAndEstadoNot(clienteId, periodoNormalizado, EstadoFactura.ANULADA)) {
            throw new IllegalStateException(
                "Ya existe una factura emitida para este cliente en el período seleccionado. " +
                "Solo se puede emitir una nueva factura si la anterior fue anulada."
            );
        }
        
        // 4. Crear período de facturación
        PeriodoFacturacion periodo = new PeriodoFacturacion(inicioPeriodo, finPeriodo);
        
        // 5. Determinar tipo de factura
        TipoFactura tipoFactura = Factura.determinarTipoFactura(
            CONDICION_IVA_EMISOR,
            cliente.getCondicionIva()
        );
        
        // 6. Obtener serie y número
        int serie = obtenerSerie(tipoFactura);
        int numero = obtenerSiguienteNumeroFactura(serie);
        
        // 7. Crear factura - usar el inicio del período como LocalDate para el campo periodo
        Factura factura = new Factura(
            serie, 
            numero, 
            cliente,
            fechaEmision, 
            fechaVencimiento, 
            inicioPeriodo, // Usar la fecha de inicio como periodo
            tipoFactura
        );
        
        // 8. Validar fechas y cliente activo
        factura.validarFechas();
        factura.validarClienteActivo();
        
        // 9. Agregar items PROPORCIONALES desde servicios contratados
        for (ServicioContratado servicioContratado : serviciosContratados) {
            Servicio servicio = servicioContratado.getServicio();
            
            // Crear item proporcional usando el método estático
            ItemFactura item = ItemFactura.crearProporcional(
                servicio.getNombre(),
                servicioContratado.getPrecioContratado(),
                1, // cantidad siempre 1 para servicios
                servicio.getAlicuotaIVA(),
                periodo
            );
            
            factura.agregarItem(item);
        }
        
        // 10. Aplicar descuento si existe
        if (porcentajeDescuento != null && porcentajeDescuento > 0) {
            if (motivoDescuento == null || motivoDescuento.isBlank()) {
                throw new IllegalArgumentException("El motivo del descuento es obligatorio");
            }
            factura.aplicarDescuento(porcentajeDescuento, motivoDescuento);
        }
        
        // 11. Persistir factura
        return facturaRepository.save(factura);
    }

    /**
     * Actualiza el estado de todas las facturas pendientes o parcialmente pagadas
     * que hayan superado su fecha de vencimiento.
     * Este método debe ser invocado periódicamente o antes de mostrar listados.
     * 
     * @return Número de facturas actualizadas a VENCIDA
     */
    @Transactional
    public int actualizarFacturasVencidas() {
        int actualizadas = 0;
        
        // Obtener facturas PENDIENTES o PAGADAS_PARCIALMENTE
        List<Factura> facturasPendientes = facturaRepository.findByEstado(EstadoFactura.PENDIENTE);
        List<Factura> facturasParciales = facturaRepository.findByEstado(EstadoFactura.PAGADA_PARCIALMENTE);
        
        // Unir ambas listas
        List<Factura> facturasARevisar = new java.util.ArrayList<>(facturasPendientes);
        facturasARevisar.addAll(facturasParciales);
        
        // Actualizar cada factura que esté vencida
        for (Factura factura : facturasARevisar) {
            if (factura.actualizarSiEstaVencida()) {
                facturaRepository.save(factura);
                actualizadas++;
            }
        }
        
        return actualizadas;
    }
}
