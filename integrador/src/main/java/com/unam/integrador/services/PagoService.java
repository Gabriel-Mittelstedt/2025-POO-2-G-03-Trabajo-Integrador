package com.unam.integrador.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unam.integrador.model.Factura;
import com.unam.integrador.model.Pago;
import com.unam.integrador.model.Recibo;
import com.unam.integrador.model.CuentaCliente;
import com.unam.integrador.model.enums.MetodoPago;
import com.unam.integrador.repositories.FacturaRepository;
import com.unam.integrador.repositories.PagoRepository;
import com.unam.integrador.repositories.ReciboRepository;
import com.unam.integrador.repositories.CuentaClienteRepositorie;
// MovimientoSaldo removed: trazabilidad a través de Pagos/Recibos

/**
 * Servicio de aplicación para la gestión de pagos.
 * 
 * Este servicio actúa como orquestador delgado, coordinando las operaciones
 * pero delegando la lógica de negocio a las entidades del dominio (modelo RICO).
 */
@Service
public class PagoService {
    
    @Autowired
    private PagoRepository pagoRepository;
    
    @Autowired
    private ReciboRepository reciboRepository;
    
    @Autowired
    private FacturaRepository facturaRepository;
    
    @Autowired
    private CuentaClienteRepositorie cuentaClienteRepository;
    
    // MovimientoSaldo entity removed — trazabilidad manejada vía Pagos y CuentaCliente
    
    /**
     * Registra un pago total de una factura.
     * Genera un recibo automáticamente.
     * 
     * @param facturaId ID de la factura a pagar
     * @param metodoPago Método de pago utilizado
     * @param referencia Referencia del pago (opcional)
     * @return El pago registrado
     */
    @Transactional
    public Pago registrarPagoTotal(Long facturaId, MetodoPago metodoPago, String referencia) {
        // 1. Obtener factura
        Factura factura = facturaRepository.findById(facturaId)
            .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada con ID: " + facturaId));
        
        // 2. Crear pago usando el factory method (modelo RICO)
        Pago pago = Pago.crearPago(factura.getTotal(), metodoPago, referencia);
        
        // 3. Registrar pago en la factura (delegar al dominio)
        factura.registrarPagoTotal(pago);
        
        // 4. Guardar pago
        Pago pagoGuardado = pagoRepository.save(pago);
        
        // 5. Generar recibo
        Recibo recibo = generarRecibo(pagoGuardado, factura);
        reciboRepository.save(recibo);
        
        // 6. Guardar factura actualizada
        facturaRepository.save(factura);
        
        return pagoGuardado;
    }

    /**
     * Registra un pago parcial de una factura.
     * Genera un recibo automáticamente.
     * 
     * @param facturaId ID de la factura a pagar
     * @param monto Monto del pago parcial
     * @param metodoPago Método de pago utilizado
     * @param referencia Referencia del pago (opcional)
     * @return El pago registrado
     */
    @Transactional
    public Pago registrarPagoParcial(Long facturaId, BigDecimal monto, MetodoPago metodoPago, String referencia) {
        // 1. Obtener factura
        Factura factura = facturaRepository.findById(facturaId)
            .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada con ID: " + facturaId));
        
        // 2. Crear pago usando el factory method (modelo RICO)
        Pago pago = Pago.crearPago(monto, metodoPago, referencia);
        
        // 3. Registrar pago parcial en la factura (delegar al dominio)
        factura.registrarPagoParcial(pago);
        
        // 4. Guardar pago
        Pago pagoGuardado = pagoRepository.save(pago);
        
        // 5. Generar recibo
        Recibo recibo = generarRecibo(pagoGuardado, factura);
        reciboRepository.save(recibo);
        
        // 6. Guardar factura actualizada
        facturaRepository.save(factura);
        
        return pagoGuardado;
    }
    
    /**
     * Lista todos los pagos registrados.
     * @return Lista de pagos
     */
    @Transactional(readOnly = true)
    public List<Pago> listarTodos() {
        return pagoRepository.findAll();
    }
    
    /**
     * Busca un pago por ID.
     * @param id ID del pago
     * @return Pago encontrado
     */
    @Transactional(readOnly = true)
    public Pago buscarPorId(Long id) {
        return pagoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado con ID: " + id));
    }
    
    /**
     * Lista pagos de una factura.
     * @param facturaId ID de la factura
     * @return Lista de pagos
     */
    @Transactional(readOnly = true)
    public List<Pago> listarPorFactura(Long facturaId) {
        return pagoRepository.findByFacturaIdFactura(facturaId);
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
    public Recibo registrarPagoCombinado(
            List<Long> facturasIds, 
            BigDecimal montoTotal, 
            BigDecimal saldoAFavorAplicar,
            MetodoPago metodoPago, 
            String referencia) {
        
        // 1. Validar parámetros
        if (saldoAFavorAplicar == null) {
            saldoAFavorAplicar = BigDecimal.ZERO;
        }
        if (montoTotal == null) {
            montoTotal = BigDecimal.ZERO;
        }
        
        BigDecimal montoTotalCombinado = montoTotal.add(saldoAFavorAplicar);
        
        if (montoTotalCombinado.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto total del pago (incluyendo saldo a favor) debe ser mayor a cero");
        }
        
        // 2. Obtener todas las facturas seleccionadas
        List<Factura> facturas = facturaRepository.findAllById(facturasIds);
        
        if (facturas.isEmpty()) {
            throw new IllegalArgumentException("No se encontraron facturas con los IDs proporcionados");
        }
        
        if (facturas.size() != facturasIds.size()) {
            throw new IllegalArgumentException("Algunas facturas no existen en el sistema");
        }
        
        // 3. Validar que todas las facturas sean del mismo cliente
        CuentaCliente cliente = facturas.get(0).getCliente();
        boolean todasDelMismoCliente = facturas.stream()
            .allMatch(f -> f.getCliente().getId().equals(cliente.getId()));
        
        if (!todasDelMismoCliente) {
            throw new IllegalArgumentException("Todas las facturas deben pertenecer al mismo cliente");
        }
        
        // 4. Validar que todas las facturas estén impagas (pendientes o pagadas parcialmente)
        boolean todasImpagas = facturas.stream()
            .allMatch(f -> f.getSaldoPendiente().compareTo(BigDecimal.ZERO) > 0);
        
        if (!todasImpagas) {
            throw new IllegalArgumentException("Solo se pueden pagar facturas con saldo pendiente");
        }
        
        // 5. Validar saldo a favor si corresponde (no descontar aún, solo validar disponibilidad)
        if (saldoAFavorAplicar.compareTo(BigDecimal.ZERO) > 0) {
            if (!cliente.tieneSaldoAFavor()) {
                throw new IllegalStateException("El cliente no tiene saldo a favor disponible");
            }

            BigDecimal saldoDisponible = cliente.getSaldoAFavor();
            if (saldoAFavorAplicar.compareTo(saldoDisponible) > 0) {
                throw new IllegalArgumentException(
                    String.format("El monto de saldo a favor a aplicar ($%s) excede el saldo disponible ($%s)", 
                        saldoAFavorAplicar, saldoDisponible));
            }

            // Validado: no descontamos todavía. El saldo se descontará por lo realmente aplicado
            // después de distribuir entre facturas para evitar inconsistencias si no se usa todo.
        }
        
        // 6. Distribuir el pago entre las facturas
        // Nuevo comportamiento: cuando el usuario ingresa un `montoTotal` en pantalla
        // y hay múltiples facturas, ese `montoTotal` se interpreta como monto por factura
        // (es decir, se intenta pagar `montoPorFactura` en cada factura, sin dividir
        // ese monto entre ellas). Cada factura recibirá hasta `montoPorFactura` o su
        // saldo pendiente, lo que sea menor.
        BigDecimal montoPorFactura = montoTotal;
        List<Pago> pagosGenerados = new ArrayList<>();
        BigDecimal montoTotalAplicadoSaldoAFavor = BigDecimal.ZERO;
        BigDecimal montoTotalAplicadoMetodoPago = BigDecimal.ZERO;

        // Calcular el total requerido para aplicar montoPorFactura en cada factura
        BigDecimal totalRequerido = BigDecimal.ZERO;
        List<BigDecimal> montosDeseadosPorFactura = new ArrayList<>();
        for (Factura factura : facturas) {
            BigDecimal saldoFactura = factura.getSaldoPendiente();
            BigDecimal deseado = saldoFactura.compareTo(montoPorFactura) >= 0 ? montoPorFactura : saldoFactura;
            montosDeseadosPorFactura.add(deseado);
            totalRequerido = totalRequerido.add(deseado);
        }

        // Si el usuario no ingresó `montoTotal` (es 0) pero sí indicó `saldoAFavorAplicar`,
        // queremos soportar el caso "pagar solo con saldo a favor". En ese caso
        // distribuimos el saldo a favor entre facturas secuencialmente (llenando
        // cada factura hasta su saldo pendiente) y no creamos pagos con el método
        // de pago seleccionado.

        // Aplicar saldo a favor (si se indicó) y luego el método de pago por factura
        BigDecimal saldoAFavorRestante = saldoAFavorAplicar;
        BigDecimal montoAplicadoTotal = BigDecimal.ZERO;

        if (montoPorFactura.compareTo(BigDecimal.ZERO) == 0 && saldoAFavorAplicar.compareTo(BigDecimal.ZERO) > 0) {
            // Distribuir únicamente saldoAFavorAplicar entre facturas (flujo "solo saldo")
            for (Factura factura : facturas) {
                if (saldoAFavorRestante.compareTo(BigDecimal.ZERO) <= 0) break;
                BigDecimal saldoFactura = factura.getSaldoPendiente();
                if (saldoFactura.compareTo(BigDecimal.ZERO) <= 0) continue;

                BigDecimal montoAplicar = saldoAFavorRestante.compareTo(saldoFactura) >= 0 ? saldoFactura : saldoAFavorRestante;
                Pago pagoSaldo = Pago.crearPago(montoAplicar, MetodoPago.SALDO_A_FAVOR, "Aplicacion de saldo a favor del cliente");
                if (montoAplicar.compareTo(saldoFactura) >= 0) {
                    factura.registrarPagoTotal(pagoSaldo);
                } else {
                    factura.registrarPagoParcial(pagoSaldo);
                }
                pagoRepository.save(pagoSaldo);
                pagosGenerados.add(pagoSaldo);
                montoTotalAplicadoSaldoAFavor = montoTotalAplicadoSaldoAFavor.add(montoAplicar);
                // Persistir inmediatamente el cambio en la cuenta del cliente para evitar estados inconsistentes
                cliente.aplicarSaldoAFavor(montoAplicar);
                cuentaClienteRepository.save(cliente);
                montoAplicadoTotal = montoAplicadoTotal.add(montoAplicar);
                saldoAFavorRestante = saldoAFavorRestante.subtract(montoAplicar);
                facturaRepository.save(factura);
            }
            // No se crean pagos con el método de pago porque montoPorFactura == 0
        } else {
            // Flujo regular: montoPorFactura > 0
        
        
        
        

        for (int i = 0; i < facturas.size(); i++) {
            Factura factura = facturas.get(i);
            BigDecimal montoDeseado = montosDeseadosPorFactura.get(i);
            if (montoDeseado.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // Primero intentar cubrir desde saldo a favor
            BigDecimal aplicarDesdeSaldo = BigDecimal.ZERO;
            if (saldoAFavorRestante.compareTo(BigDecimal.ZERO) > 0) {
                aplicarDesdeSaldo = saldoAFavorRestante.compareTo(montoDeseado) >= 0
                    ? montoDeseado
                    : saldoAFavorRestante;
            }

            if (aplicarDesdeSaldo.compareTo(BigDecimal.ZERO) > 0) {
                Pago pagoSaldo = Pago.crearPago(aplicarDesdeSaldo, MetodoPago.SALDO_A_FAVOR,
                    "Aplicacion de saldo a favor del cliente");
                if (aplicarDesdeSaldo.compareTo(factura.getSaldoPendiente()) >= 0) {
                    factura.registrarPagoTotal(pagoSaldo);
                } else {
                    factura.registrarPagoParcial(pagoSaldo);
                }
                pagoRepository.save(pagoSaldo);
                pagosGenerados.add(pagoSaldo);
                montoTotalAplicadoSaldoAFavor = montoTotalAplicadoSaldoAFavor.add(aplicarDesdeSaldo);
                // Persistir inmediatamente el cambio en la cuenta del cliente
                cliente.aplicarSaldoAFavor(aplicarDesdeSaldo);
                cuentaClienteRepository.save(cliente);
                saldoAFavorRestante = saldoAFavorRestante.subtract(aplicarDesdeSaldo);
                montoAplicadoTotal = montoAplicadoTotal.add(aplicarDesdeSaldo);
            }

            // Si queda por cubrir en esta factura, cubrir con el metodo de pago
            BigDecimal restantePorFactura = montoDeseado.subtract(aplicarDesdeSaldo);
            if (restantePorFactura.compareTo(BigDecimal.ZERO) > 0) {
                Pago pagoMetodo = Pago.crearPago(restantePorFactura, metodoPago, referencia);
                if (restantePorFactura.compareTo(factura.getSaldoPendiente()) >= 0) {
                    factura.registrarPagoTotal(pagoMetodo);
                } else {
                    factura.registrarPagoParcial(pagoMetodo);
                }
                pagoRepository.save(pagoMetodo);
                pagosGenerados.add(pagoMetodo);
                montoTotalAplicadoMetodoPago = montoTotalAplicadoMetodoPago.add(restantePorFactura);
                montoAplicadoTotal = montoAplicadoTotal.add(restantePorFactura);
            }

            facturaRepository.save(factura);
        }
        // 7. Descontar del cliente exactamente lo que se aplicó desde su saldo a favor
        if (montoTotalAplicadoSaldoAFavor.compareTo(BigDecimal.ZERO) > 0) {
            cliente.aplicarSaldoAFavor(montoTotalAplicadoSaldoAFavor);
            cuentaClienteRepository.save(cliente);
        }
    }

        // 8. Si sobra dinero (el total aportado por el usuario + saldo a favor excede
        // lo requerido), registrar el sobrante como saldo a favor del cliente.
        BigDecimal montoSobrante = montoTotalCombinado.subtract(montoAplicadoTotal);
        if (montoSobrante.compareTo(BigDecimal.ZERO) > 0) {
            cliente.registrarSaldoAFavor(montoSobrante);
            cuentaClienteRepository.save(cliente);
        }

        // Generar un único recibo que agrupe todas las facturas pagadas
        StringBuilder facturasInfo = new StringBuilder();
        facturasInfo.append(facturas.stream()
            .map(f -> String.format("Factura %d-%08d ($%s)", 
                f.getSerie(), f.getNroFactura(), f.getTotal()))
            .collect(Collectors.joining(", ")));

        if (montoTotalAplicadoSaldoAFavor.compareTo(BigDecimal.ZERO) > 0) {
            facturasInfo.append(String.format(" - Saldo a favor aplicado: $%s", montoTotalAplicadoSaldoAFavor));
        }
        if (montoSobrante.compareTo(BigDecimal.ZERO) > 0) {
            facturasInfo.append(String.format(" - Nuevo saldo a favor: $%s", montoSobrante));
        }

        // Generar número de recibo
        int ultimoNumero = reciboRepository.findUltimoNumeroRecibo();
        String numero = String.format("%08d", ultimoNumero + 1);

        // Crear el recibo usando el factory method del modelo rico. El monto del recibo
        // será el monto efectivamente aplicado (montoAplicadoTotal).
        Recibo recibo = Recibo.crearRecibo(
            numero, 
            montoAplicadoTotal, 
            metodoPago, 
            referencia, 
            facturasInfo.toString()
        );
        
        // Asociar el recibo al primer pago (por convención)
        if (!pagosGenerados.isEmpty()) {
            recibo.asociarPago(pagosGenerados.get(0));
        }
        
        return reciboRepository.save(recibo);
    }
    
    /**
     * Lista las facturas impagas de un cliente (para pago combinado).
     * @param clienteId ID del cliente
     * @return Lista de facturas impagas
     */
    @Transactional(readOnly = true)
    public List<Factura> listarFacturasImpagasPorCliente(Long clienteId) {
        return facturaRepository.findFacturasImpagasByCliente(clienteId);
    }
    
    /**
     * Aplica saldo a favor de un cliente a una o m\u00e1s facturas impagas.
     * HU-13: Gesti\u00f3n de saldo a favor.
     * 
     * @param clienteId ID del cliente
     * @param facturasIds IDs de las facturas a las que se aplicar\u00e1 el saldo
     * @return Recibo generado con el detalle de la aplicaci\u00f3n
     * @throws IllegalArgumentException si hay errores en las validaciones
     * @throws IllegalStateException si el cliente no tiene saldo a favor suficiente
     */
    @Transactional
    public Recibo aplicarSaldoAFavor(Long clienteId, List<Long> facturasIds) {
        // 1. Obtener cliente y validar que tenga saldo a favor
        CuentaCliente cliente = cuentaClienteRepository.findById(clienteId)
            .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado con ID: " + clienteId));
        
        if (!cliente.tieneSaldoAFavor()) {
            throw new IllegalStateException("El cliente no tiene saldo a favor disponible");
        }
        
        BigDecimal saldoDisponible = cliente.getSaldoAFavor();
        
        // 2. Obtener facturas
        List<Factura> facturas = facturaRepository.findAllById(facturasIds);
        
        if (facturas.isEmpty()) {
            throw new IllegalArgumentException("No se encontraron facturas con los IDs proporcionados");
        }
        
        // 3. Validar que todas las facturas sean del cliente
        boolean todasDelCliente = facturas.stream()
            .allMatch(f -> f.getCliente().getId().equals(clienteId));
        
        if (!todasDelCliente) {
            throw new IllegalArgumentException("Todas las facturas deben pertenecer al cliente seleccionado");
        }
        
        // 4. Validar que todas las facturas tengan saldo pendiente
        boolean todasImpagas = facturas.stream()
            .allMatch(f -> f.getSaldoPendiente().compareTo(BigDecimal.ZERO) > 0);
        
        if (!todasImpagas) {
            throw new IllegalArgumentException("Solo se puede aplicar saldo a facturas con saldo pendiente");
        }
        
        // 5. Aplicar el saldo a las facturas
        BigDecimal saldoRestante = saldoDisponible;
        BigDecimal montoTotalAplicado = BigDecimal.ZERO;
        List<Pago> pagosGenerados = new ArrayList<>();
        
        for (Factura factura : facturas) {
            if (saldoRestante.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            
            BigDecimal saldoFactura = factura.getSaldoPendiente();
            BigDecimal montoAplicar = saldoRestante.compareTo(saldoFactura) >= 0
                ? saldoFactura
                : saldoRestante;
            
            // Crear pago con método SALDO_A_FAVOR
            Pago pago = Pago.crearPago(montoAplicar, MetodoPago.SALDO_A_FAVOR, 
                "Aplicacion de saldo a favor del cliente");
            
            // Registrar pago en la factura
            if (montoAplicar.compareTo(saldoFactura) >= 0) {
                factura.registrarPagoTotal(pago);
            } else {
                factura.registrarPagoParcial(pago);
            }
            
            pagoRepository.save(pago);
            facturaRepository.save(factura);
            
            pagosGenerados.add(pago);
            montoTotalAplicado = montoTotalAplicado.add(montoAplicar);
            saldoRestante = saldoRestante.subtract(montoAplicar);
        }
        
        // 6. Actualizar el saldo del cliente (la trazabilidad queda en los Pagos y Recibos generados)
        cliente.aplicarSaldoAFavor(montoTotalAplicado);
        cuentaClienteRepository.save(cliente);
        
        // 7. Generar recibo
        String facturasInfo = facturas.stream()
            .map(f -> String.format("Factura %d-%08d", f.getSerie(), f.getNroFactura()))
            .collect(Collectors.joining(", "));
        
        facturasInfo += String.format(" - Aplicacion de saldo a favor: $%s", montoTotalAplicado);
        
        int ultimoNumero = reciboRepository.findUltimoNumeroRecibo();
        String numero = String.format("%08d", ultimoNumero + 1);
        
        Recibo recibo = Recibo.crearRecibo(
            numero,
            montoTotalAplicado,
            MetodoPago.SALDO_A_FAVOR,
            "Aplicacion de saldo a favor",
            facturasInfo
        );
        
        if (!pagosGenerados.isEmpty()) {
            recibo.asociarPago(pagosGenerados.get(0));
        }
        
        Recibo reciboGuardado = reciboRepository.save(recibo);
        // No hay MovimientoSaldo; retornamos el recibo generado
        return reciboGuardado;
    }
    
    // --- Métodos privados auxiliares ---
    
    /**
     * Genera un recibo para un pago.
     */
    private Recibo generarRecibo(Pago pago, Factura factura) {
        // Generar número de recibo
        int ultimoNumero = reciboRepository.findUltimoNumeroRecibo();
        String numero = String.format("%08d", ultimoNumero + 1);

        // Asociar facturas
        String facturaInfo = String.format("Factura %d-%08d", 
            factura.getSerie(), factura.getNroFactura());

        // Crear recibo a través del factory del modelo rico
        Recibo recibo = Recibo.crearRecibo(numero, pago.getMonto(), pago.getMetodoPago(), pago.getReferencia(), facturaInfo);

        // Asociar pago y mantener consistencia bidireccional
        recibo.asociarPago(pago);

        return recibo;
    }
}
