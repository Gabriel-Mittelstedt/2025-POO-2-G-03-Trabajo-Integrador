package com.unam.integrador.services;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unam.integrador.model.CuentaCliente;
import com.unam.integrador.model.Factura;
import com.unam.integrador.model.ItemFactura;
import com.unam.integrador.model.LoteFacturacion;
import com.unam.integrador.model.NotaCredito;
import com.unam.integrador.model.PeriodoFacturacion;
import com.unam.integrador.model.Servicio;
import com.unam.integrador.model.ServicioContratado;
import com.unam.integrador.model.enums.EstadoCuenta;
import com.unam.integrador.model.enums.EstadoFactura;
import com.unam.integrador.model.enums.TipoCondicionIVA;
import com.unam.integrador.model.enums.TipoFactura;
import com.unam.integrador.repositories.CuentaClienteRepositorie;
import com.unam.integrador.repositories.FacturaRepository;
import com.unam.integrador.repositories.LoteFacturacionRepository;
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
    
    @Autowired
    private LoteFacturacionRepository loteFacturacionRepository;
    
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
        
        // 9. Agregar items solo de servicios facturables (modelo rico)
        for (ServicioContratado servicioContratado : serviciosContratados) {
            Servicio servicio = servicioContratado.getServicio();
            
            // Solo facturar servicios activos
            if (servicio != null && servicio.puedeFacturarse()) {
                ItemFactura item = new ItemFactura(
                    servicio.getNombre(),                       // descripcion
                    servicioContratado.getPrecioContratado(),   // precioUnitario (precio específico del contrato)
                    1,                                          // cantidad (siempre 1 para servicios mensuales)
                    servicio.getAlicuotaIVA()                   // alicuotaIVA
                );
                
                factura.agregarItem(item);
            }
        }
        
        // Validar que se haya agregado al menos un item
        if (factura.getDetalleFactura().isEmpty()) {
            throw new IllegalArgumentException(
                "No se puede emitir una factura sin servicios activos. "
                + "Todos los servicios contratados están inactivos."
            );
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
        Factura ultimaFactura = facturaRepository.findFirstBySerieOrderByNroFacturaDesc(serie);
        return (ultimaFactura != null) ? ultimaFactura.getNroFactura() + 1 : 1;
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
        NotaCredito ultimaNota = notaCreditoRepository.findFirstBySerieOrderByNroNotaCreditoDesc(serie);
        return (ultimaNota != null) ? ultimaNota.getNroNotaCredito() + 1 : 1;
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
    
    // ========== MÉTODOS DE FACTURACIÓN MASIVA (HU-07, HU-08, HU-09) ==========
    
    /**
     * Ejecuta la facturación masiva para un período determinado.
     * Genera facturas para todos los clientes activos con servicios contratados.
     * 
     * Implementa HU-07: Emisión de facturación masiva por período
     * 
     * @param periodoStr Período en formato texto (ej: "Noviembre 2025")
     * @param fechaVencimiento Fecha de vencimiento para todas las facturas
     * @return Lote de facturación generado con todas las facturas
     * @throws IllegalStateException si ya existe un lote para el período
     */
    @Transactional
    public LoteFacturacion ejecutarFacturacionMasiva(
            String periodoStr,
            LocalDate fechaVencimiento) {
        
        // 1. Validar parámetros
        if (periodoStr == null || periodoStr.trim().isEmpty()) {
            throw new IllegalArgumentException("El período es obligatorio");
        }
        if (fechaVencimiento == null) {
            throw new IllegalArgumentException("La fecha de vencimiento es obligatoria");
        }
        
        // 2. Convertir período string a LocalDate
        LocalDate periodoFecha = convertirPeriodoALocalDate(periodoStr);
        LocalDate fechaEmision = LocalDate.now();
        
        // 3. Validar que la fecha de vencimiento sea posterior a la fecha de emisión
        if (fechaVencimiento.isBefore(fechaEmision) || fechaVencimiento.isEqual(fechaEmision)) {
            throw new IllegalArgumentException(
                "La fecha de vencimiento debe ser posterior a la fecha de emisión (" + fechaEmision + ")"
            );
        }
        
        // 4. Verificar que no exista un lote activo para el mismo período
        if (loteFacturacionRepository.existsByPeriodoFechaAndAnuladoFalse(periodoFecha)) {
            throw new IllegalStateException(
                "Ya existe una facturación masiva activa para el período " + periodoStr + 
                ". Debe anular el lote existente antes de crear uno nuevo."
            );
        }
        
        // 5. Crear el lote de facturación (sin usuario)
        LoteFacturacion lote = new LoteFacturacion(
            periodoStr,
            periodoFecha,
            fechaVencimiento
        );
        
        // 6. Obtener todos los clientes activos con servicios contratados
        List<CuentaCliente> clientesActivos = clienteRepository.findAll().stream()
            .filter(c -> c.getEstado() == EstadoCuenta.ACTIVA)
            .filter(c -> !c.getServiciosContratadosActivos().isEmpty())
            .collect(Collectors.toList());
        
        if (clientesActivos.isEmpty()) {
            throw new IllegalStateException(
                "No hay clientes activos con servicios contratados para facturar en el período " + periodoStr
            );
        }
        
        // 7. Generar factura para cada cliente
        List<String> errores = new ArrayList<>();
        int facturasGeneradas = 0;
        
        for (CuentaCliente cliente : clientesActivos) {
            try {
                // Verificar si ya existe factura para este cliente en este período
                if (facturaRepository.existsByClienteIdAndPeriodoAndEstadoNot(
                        cliente.getId(), periodoFecha, EstadoFactura.ANULADA)) {
                    errores.add("Cliente " + cliente.getNombre() + " ya tiene factura para este período");
                    continue;
                }
                
                // Determinar tipo de factura
                TipoFactura tipoFactura = Factura.determinarTipoFactura(
                    CONDICION_IVA_EMISOR,
                    cliente.getCondicionIva()
                );
                
                // Obtener serie y número
                int serie = obtenerSerie(tipoFactura);
                int numero = obtenerSiguienteNumeroFactura(serie);
                
                // Crear factura
                Factura factura = new Factura(
                    serie,
                    numero,
                    cliente,
                    fechaEmision,
                    fechaVencimiento,
                    periodoFecha,
                    tipoFactura
                );
                
                // Agregar items desde servicios contratados
                for (ServicioContratado servicioContratado : cliente.getServiciosContratadosActivos()) {
                    Servicio servicio = servicioContratado.getServicio();
                    
                    ItemFactura item = new ItemFactura(
                        servicio.getNombre(),
                        servicioContratado.getPrecioContratado(),
                        1,
                        servicio.getAlicuotaIVA()
                    );
                    
                    factura.agregarItem(item);
                }
                
                // Agregar factura al lote
                lote.agregarFactura(factura);
                facturasGeneradas++;
                
            } catch (Exception e) {
                errores.add("Error al generar factura para cliente " + cliente.getNombre() + ": " + e.getMessage());
            }
        }
        
        // 8. Verificar que se haya generado al menos una factura
        if (facturasGeneradas == 0) {
            String mensajeError = "No se pudo generar ninguna factura.";
            if (!errores.isEmpty()) {
                mensajeError += " Errores: " + String.join("; ", errores);
            }
            throw new IllegalStateException(mensajeError);
        }
        
        // 9. Guardar el lote con todas sus facturas
        lote = loteFacturacionRepository.save(lote);
        
        return lote;
    }
    
    /**
     * Convierte un string de período en formato "Mes Año" a LocalDate.
     * El día siempre será 1.
     * 
     * @param periodoStr String del período (ej: "Noviembre 2025")
     * @return LocalDate con el primer día del mes indicado
     */
    private LocalDate convertirPeriodoALocalDate(String periodoStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.of("es", "ES"));
        YearMonth yearMonth = YearMonth.parse(periodoStr.toLowerCase(), formatter);
        return yearMonth.atDay(1);
    }
    
    /**
     * Obtiene todos los lotes de facturación ordenados por fecha de ejecución.
     * 
     * @return Lista de lotes ordenados de más reciente a más antiguo
     */
    @Transactional(readOnly = true)
    public List<LoteFacturacion> listarLotesFacturacion() {
        return loteFacturacionRepository.findAllByOrderByFechaEjecucionDesc();
    }
    
    /**
     * Obtiene un lote de facturación por su ID.
     * 
     * @param id ID del lote
     * @return Lote encontrado
     * @throws IllegalArgumentException si no existe
     */
    @Transactional(readOnly = true)
    public LoteFacturacion obtenerLotePorId(Long id) {
        return loteFacturacionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Lote de facturación no encontrado con ID: " + id));
    }
    
    /**
     * Anula un lote de facturación completo.
     * Genera notas de crédito para todas las facturas del lote.
     * 
     * Implementa HU-08: Anulación de facturación masiva
     * 
     * @param loteId ID del lote a anular
     * @param motivo Motivo de la anulación
     * @return Lote anulado
     * @throws IllegalStateException si el lote no puede ser anulado
     */
    @Transactional
    public LoteFacturacion anularLoteFacturacion(Long loteId, String motivo) {
        // 1. Obtener el lote
        LoteFacturacion lote = obtenerLotePorId(loteId);
        
        // 2. Validar que puede ser anulado
        if (!lote.puedeSerAnulado()) {
            throw new IllegalStateException(
                "No se puede anular el lote. Algunas facturas ya tienen pagos registrados."
            );
        }
        
        // 3. Anular cada factura del lote y generar notas de crédito
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
                    motivo + " (Anulación de lote #" + loteId + ")",
                    factura.getTipo(),
                    factura
                );
                
                factura.agregarNotaCredito(notaCredito);
                factura.anular();
                
                notaCreditoRepository.save(notaCredito);
                facturaRepository.save(factura);
            }
        }
        
        // 4. Anular el lote
        lote.anular(motivo);
        
        // 5. Guardar y retornar
        return loteFacturacionRepository.save(lote);
    }
    
    /**
     * Obtiene el resumen de un lote de facturación.
     * Útil para la vista de detalle (HU-09).
     * 
     * @param loteId ID del lote
     * @return Lote con sus facturas cargadas
     */
    @Transactional(readOnly = true)
    public LoteFacturacion obtenerLoteConFacturas(Long loteId) {
        LoteFacturacion lote = obtenerLotePorId(loteId);
        // Forzar carga de facturas (lazy loading)
        lote.getFacturas().size();
        return lote;
    }
}
