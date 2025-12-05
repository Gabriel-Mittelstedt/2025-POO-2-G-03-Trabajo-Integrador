package com.unam.integrador.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unam.integrador.model.CuentaCliente;
import com.unam.integrador.model.DetallePago;
import com.unam.integrador.model.Factura;
import com.unam.integrador.model.Pago;
import com.unam.integrador.model.enums.EstadoFactura;
import com.unam.integrador.model.enums.MetodoPago;
import com.unam.integrador.repositories.CuentaClienteRepositorie;
import com.unam.integrador.repositories.DetallePagoRepository;
import com.unam.integrador.repositories.FacturaRepository;
import com.unam.integrador.repositories.PagoRepository;

/**
 * Servicio de aplicación para la gestión de pagos.
 * 
 * Responsabilidad: únicamente
 * - Coordina repositorios y transacciones
 * - Genera números de recibo
 * - NO contiene lógica de negocio (delegada a entidades)
 * 
 * La lógica de negocio reside en:
 * - Factura: validaciones, cálculo de estado
 * - Pago: validaciones, factory method
 * - DetallePago: validaciones de integridad
 */
@Service
public class PagoService {
    
    @Autowired
    private PagoRepository pagoRepository;
    
    @Autowired
    private FacturaRepository facturaRepository;
    
    @Autowired
    private CuentaClienteRepositorie cuentaClienteRepository;
    
    @Autowired
    private DetallePagoRepository detallePagoRepository;

    
    /**
     * Lista todos los pagos registrados.
     * @return Lista de pagos
     */
    @Transactional(readOnly = true)
    public List<Pago> listarTodos() {
        return pagoRepository.findAll();
    }

    /**
     * Lista pagos filtrados por nombre de cliente y rango de fechas (fechaPago).
     * Los parámetros son opcionales; si son null se ignoran.
     *
     * @param clienteNombre filtro por nombre (contiene, case-insensitive)
     * @param desde fecha inicial (inclusive)
     * @param hasta fecha final (inclusive)
     * @return lista de pagos que cumplen los filtros
     */
    @Transactional(readOnly = true)
    public List<Pago> listarFiltrados(String clienteNombre, LocalDate desde, LocalDate hasta) {
        List<Pago> todosPagos = pagoRepository.findAll();
        List<Pago> pagosFiltrados = new ArrayList<>();
        
        for (Pago pago : todosPagos) {
            boolean cumpleFiltros = true;
            // Filtro por fecha desde
            if (desde != null) {
                if (pago.getFechaPago() == null || pago.getFechaPago().isBefore(desde)) {
                    cumpleFiltros = false;
                }
            }
            // Filtro por fecha hasta
            if (hasta != null && cumpleFiltros) {
                if (pago.getFechaPago() == null || pago.getFechaPago().isAfter(hasta)) {
                    cumpleFiltros = false;
                }
            }
            // Filtro por cliente
            if (clienteNombre != null && !clienteNombre.isBlank() && cumpleFiltros) {
                List<DetallePago> detalles = detallePagoRepository.findByPagoIdPago(pago.getIdPago());
                boolean clienteEncontrado = false;
                
                for (DetallePago detalle : detalles) {
                    Factura factura = detalle.getFactura();
                    if (factura != null && factura.getCliente() != null && 
                        factura.getCliente().getNombre() != null) {
                        String nombreCliente = factura.getCliente().getNombre().toLowerCase();
                        if (nombreCliente.contains(clienteNombre.toLowerCase())) {
                            clienteEncontrado = true;
                            break;
                        }
                    }
                }
                if (!clienteEncontrado) {
                    cumpleFiltros = false;
                }
            }
            if (cumpleFiltros) {
                pagosFiltrados.add(pago);
            }
        }
        
        return pagosFiltrados;
    }
    
    /**
     * Busca un pago por ID.
     * @param id ID del pago
     * @return Pago encontrado
     */
    @Transactional(readOnly = true)
    public Pago buscarPorId(Long id) {
        Pago pago = pagoRepository.findById(id).orElse(null);
        if (pago == null) {
            throw new IllegalArgumentException("Pago no encontrado con ID: " + id);
        }
        return pago;
    }

    /**
     * Lista pagos de una factura.
     * @param facturaId ID de la factura
     * @return Lista de pagos
     */
    @Transactional(readOnly = true)
    public List<Pago> listarPorFactura(Long facturaId) {
        List<DetallePago> detalles = detallePagoRepository.findByFacturaIdFactura(facturaId);
        List<Pago> pagos = new ArrayList<>();
        
        for (DetallePago detalle : detalles) {
            Pago pago = detalle.getPago();
            if (!pagos.contains(pago)) {
                pagos.add(pago);
            }
        }
        
        return pagos;
    }
    
    /**
     * Registra un pago combinado que se aplica a múltiples facturas de un mismo cliente.
     * 
     * El método orquesta las entidades de dominio:
     * 1. Valida que todas las facturas sean del mismo cliente y estén impagas
     * 2. Si se proporciona saldoAFavorAplicar, primero aplica ese monto del saldo a favor del cliente
     * 3. Distribuye el monto del pago (con método de pago) entre las facturas seleccionadas
     * 4. Actualiza el saldo pendiente de cada factura (delegando a la entidad Factura)
     * 5. Si sobra dinero, crea un saldo a favor para el cliente
     * 6. Genera un único recibo con el detalle de todas las facturas pagadas
     * 
     * @param facturasIds Lista de IDs de las facturas a pagar
     * @param montoTotal Monto total del pago recibido con el método de pago
     * @param saldoAFavorAplicar Monto del saldo a favor del cliente que se aplicará
     * @param metodoPago Método de pago utilizado
     * @param referencia Referencia o comprobante del pago (opcional)
     * @return El recibo generado
     * @throws IllegalArgumentException si hay errores en las validaciones
     */
    @Transactional
    public String registrarPagoCombinado(
            List<Long> facturasIds, 
            BigDecimal montoTotal, 
            BigDecimal saldoAFavorAplicar,
            MetodoPago metodoPago, 
            String referencia) {
        
        // 1. Validar y normalizar parámetros
        if (saldoAFavorAplicar == null) {
            saldoAFavorAplicar = BigDecimal.ZERO;
        }
        if (montoTotal == null) {
            montoTotal = BigDecimal.ZERO;
        }
        BigDecimal dineroTotal = montoTotal.add(saldoAFavorAplicar);
        
        if (dineroTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto total debe ser mayor a cero");
        }
        
        // 2. Obtener facturas y cliente
        List<Factura> facturas = facturaRepository.findAllById(facturasIds);
        if (facturas.isEmpty()) {
            throw new IllegalArgumentException("No se encontraron facturas");
        }
        
        // Ordenar facturas por fecha de emisión (más antiguas primero)
        Collections.sort(facturas, new Comparator<Factura>() {
            @Override
            public int compare(Factura f1, Factura f2) {
                return f1.getFechaEmision().compareTo(f2.getFechaEmision());
            }
        });
        
        CuentaCliente cliente = facturas.get(0).getCliente();
        
        // 3. Descontar saldo a favor del cliente
        if (saldoAFavorAplicar.compareTo(BigDecimal.ZERO) > 0) {
            cliente.aplicarSaldoAFavor(saldoAFavorAplicar);
            cuentaClienteRepository.save(cliente);
        }
        
        // 4. Crear pagos únicos (máximo 2: uno por saldo a favor y otro por método de pago)
        List<Pago> pagosGenerados = new ArrayList<>();
        String numeroRecibo = generarNumeroReciboSecuencial();
        
        Pago pagoSaldoAFavor = null;
        Pago pagoMetodoPago = null;
        
        if (saldoAFavorAplicar.compareTo(BigDecimal.ZERO) > 0) {
            pagoSaldoAFavor = Pago.crearPago(saldoAFavorAplicar, MetodoPago.SALDO_A_FAVOR, null);
            pagoSaldoAFavor.setNumeroRecibo(numeroRecibo);
            pagoRepository.save(pagoSaldoAFavor);
            pagosGenerados.add(pagoSaldoAFavor);
        }
        
        if (montoTotal.compareTo(BigDecimal.ZERO) > 0) {
            pagoMetodoPago = Pago.crearPago(montoTotal, metodoPago, referencia);
            pagoMetodoPago.setNumeroRecibo(numeroRecibo);
            pagoRepository.save(pagoMetodoPago);
            pagosGenerados.add(pagoMetodoPago);
        }
        
        // 5. Distribuir los pagos entre las facturas usando DetallePago
        BigDecimal saldoAFavorRestante = saldoAFavorAplicar;
        BigDecimal dineroRestante = montoTotal;
        
        for (Factura factura : facturas) {
            BigDecimal totalDisponible = saldoAFavorRestante.add(dineroRestante);
            if (totalDisponible.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            
            BigDecimal montoPorPagar = totalDisponible.min(factura.getSaldoPendiente());
            BigDecimal montoConSaldoAFavor = montoPorPagar.min(saldoAFavorRestante);
            BigDecimal montoConMetodo = montoPorPagar.subtract(montoConSaldoAFavor);
            
            // Crear detalles de pago para esta factura
            if (montoConSaldoAFavor.compareTo(BigDecimal.ZERO) > 0 && pagoSaldoAFavor != null) {
                detallePagoRepository.save(factura.registrarPago(pagoSaldoAFavor, montoConSaldoAFavor));
            }
            
            if (montoConMetodo.compareTo(BigDecimal.ZERO) > 0 && pagoMetodoPago != null) {
                detallePagoRepository.save(factura.registrarPago(pagoMetodoPago, montoConMetodo));
            }
            
            saldoAFavorRestante = saldoAFavorRestante.subtract(montoConSaldoAFavor);
            dineroRestante = dineroRestante.subtract(montoConMetodo);
            facturaRepository.save(factura);
        }
        
        // 6. Registrar excedente como saldo a favor del cliente
        BigDecimal totalRestante = saldoAFavorRestante.add(dineroRestante);
        if (totalRestante.compareTo(BigDecimal.ZERO) > 0) {
            cliente.registrarSaldoAFavor(totalRestante);
            cuentaClienteRepository.save(cliente);
        }
        
        // 7. Retornar número de recibo
        return numeroRecibo;
    }
    
    /**
     * Lista las facturas impagas de un cliente (para pago combinado).
     * @param clienteId ID del cliente
     * @return Lista de facturas impagas
     */
    @Transactional(readOnly = true)
    public List<Factura> listarFacturasImpagasPorCliente(Long clienteId) {
        List<EstadoFactura> estadosImpagas = List.of(
            EstadoFactura.PENDIENTE, 
            EstadoFactura.VENCIDA, 
            EstadoFactura.PAGADA_PARCIALMENTE
        );
        return facturaRepository.findByClienteIdAndEstadoInOrderByFechaEmisionAsc(clienteId, estadosImpagas);
    }
    
    /**
     * Aplica saldo a favor de un cliente a una o mas facturas impagas.
     * 
     * @param clienteId ID del cliente
     * @param facturasIds IDs de las facturas a las que se aplicar\u00e1 el saldo
     * @return Recibo generado con el detalle de la aplicaci\u00f3n
     * @throws IllegalArgumentException si hay errores en las validaciones
     * @throws IllegalStateException si el cliente no tiene saldo a favor suficiente
     */
    @Transactional
    public String aplicarSaldoAFavor(Long clienteId, List<Long> facturasIds) {
        // 1. Obtener cliente y facturas
        CuentaCliente cliente = cuentaClienteRepository.findById(clienteId).orElse(null);
        if (cliente == null) {
            throw new IllegalArgumentException("Cliente no encontrado");
        }
        
        List<Factura> facturas = facturaRepository.findAllById(facturasIds);
        if (facturas.isEmpty()) {
            throw new IllegalArgumentException("No se encontraron facturas");
        }
        
        // 2. Crear un único pago con el saldo a favor total aplicado
        BigDecimal saldoDisponible = cliente.getSaldoAFavor();
        BigDecimal montoTotalAAplicar = BigDecimal.ZERO;
        
        // Calcular cuánto saldo se puede aplicar
        for (Factura factura : facturas) {
            BigDecimal montoAplicable = saldoDisponible.subtract(montoTotalAAplicar).min(factura.getSaldoPendiente());
            if (montoAplicable.compareTo(BigDecimal.ZERO) > 0) {
                montoTotalAAplicar = montoTotalAAplicar.add(montoAplicable);
            }
        }
        
        if (montoTotalAAplicar.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("No hay saldo a favor suficiente para aplicar");
        }
        
        // Crear el pago único
        String numeroRecibo = generarNumeroReciboSecuencial();
        Pago pago = Pago.crearPago(montoTotalAAplicar, MetodoPago.SALDO_A_FAVOR, null);
        pago.setNumeroRecibo(numeroRecibo);
        pagoRepository.save(pago);
        
        // 3. Distribuir el pago entre las facturas
        BigDecimal saldoRestante = montoTotalAAplicar;
        
        for (Factura factura : facturas) {
            if (saldoRestante.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            
            BigDecimal montoAplicar = saldoRestante.min(factura.getSaldoPendiente());
            detallePagoRepository.save(factura.registrarPago(pago, montoAplicar));
            facturaRepository.save(factura);
            
            saldoRestante = saldoRestante.subtract(montoAplicar);
        }
        
        // 4. Actualizar saldo del cliente
        cliente.aplicarSaldoAFavor(montoTotalAAplicar);
        cuentaClienteRepository.save(cliente);
        
        // 5. Retornar número de recibo
        return numeroRecibo;
    }
    
    // --- Métodos privados auxiliares ---
    
    /**
     * Genera un número de recibo secuencial basado en números de recibo únicos existentes.
     * Cuenta cuántos números de recibo distintos hay y genera el siguiente.
     */
    private String generarNumeroReciboSecuencial() {
        List<Pago> todosPagos = pagoRepository.findAll();
        List<String> numerosUnicos = new ArrayList<>();
        
        for (Pago pago : todosPagos) {
            String numeroRecibo = pago.getNumeroRecibo();
            if (numeroRecibo != null && !numeroRecibo.isEmpty() && !numerosUnicos.contains(numeroRecibo)) {
                numerosUnicos.add(numeroRecibo);
            }
        }
        
        return String.format("%08d", numerosUnicos.size() + 1);
    }
}