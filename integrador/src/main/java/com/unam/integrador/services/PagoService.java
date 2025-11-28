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
     * 2. Distribuye el monto del pago entre las facturas seleccionadas
     * 3. Actualiza el saldo pendiente de cada factura (delegando a la entidad Factura)
     * 4. Si sobra dinero, crea un saldo a favor para el cliente
     * 5. Genera un único recibo con el detalle de todas las facturas pagadas
     * 
     * @param facturasIds Lista de IDs de las facturas a pagar
     * @param montoTotal Monto total del pago recibido
     * @param metodoPago Método de pago utilizado
     * @param referencia Referencia o comprobante del pago (opcional)
     * @return El recibo generado
     * @throws IllegalArgumentException si hay errores en las validaciones
     */
    @Transactional
    public Recibo registrarPagoCombinado(
            List<Long> facturasIds, 
            BigDecimal montoTotal, 
            MetodoPago metodoPago, 
            String referencia) {
        
        // 1. Validar que el monto sea positivo
        if (montoTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del pago debe ser mayor a cero");
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
        
        // 5. Distribuir el pago entre las facturas
        BigDecimal montoRestante = montoTotal;
        List<Pago> pagosGenerados = new ArrayList<>();
        
        for (Factura factura : facturas) {
            if (montoRestante.compareTo(BigDecimal.ZERO) <= 0) {
                break; // Ya no hay más dinero para distribuir
            }
            
            // Determinar cuánto pagar de esta factura
            BigDecimal saldoFactura = factura.getSaldoPendiente();
            BigDecimal montoPago = montoRestante.compareTo(saldoFactura) >= 0 
                ? saldoFactura  // Pagar el saldo completo
                : montoRestante; // Pagar lo que queda
            
            // Crear el pago usando el factory method del modelo rico
            Pago pago = Pago.crearPago(montoPago, metodoPago, referencia);
            
            // Registrar el pago en la factura (delegar al dominio)
            if (montoPago.compareTo(saldoFactura) >= 0) {
                factura.registrarPagoTotal(pago);
            } else {
                factura.registrarPagoParcial(pago);
            }
            
            // Guardar el pago y la factura actualizada
            pagoRepository.save(pago);
            facturaRepository.save(factura);
            
            pagosGenerados.add(pago);
            montoRestante = montoRestante.subtract(montoPago);
        }
        
        // 6. Si sobra dinero, crear saldo a favor
        if (montoRestante.compareTo(BigDecimal.ZERO) > 0) {
            // Actualizar el saldo del cliente (negativo = a favor del cliente)
            BigDecimal saldoActual = cliente.getSaldo() != null ? cliente.getSaldo() : BigDecimal.ZERO;
            cliente.setSaldo(saldoActual.subtract(montoRestante));
            cuentaClienteRepository.save(cliente);
        }
        
        // 7. Generar un único recibo que agrupe todas las facturas pagadas
        String facturasInfo = facturas.stream()
            .map(f -> String.format("Factura %d-%08d ($%s)", 
                f.getSerie(), f.getNroFactura(), f.getTotal()))
            .collect(Collectors.joining(", "));
        
        if (montoRestante.compareTo(BigDecimal.ZERO) > 0) {
            facturasInfo += String.format(" - Saldo a favor: $%s", montoRestante);
        }
        
        // Generar número de recibo
        int ultimoNumero = reciboRepository.findUltimoNumeroRecibo();
        String numero = String.format("%08d", ultimoNumero + 1);
        
        // Crear el recibo usando el factory method del modelo rico
        Recibo recibo = Recibo.crearRecibo(
            numero, 
            montoTotal, 
            metodoPago, 
            referencia, 
            facturasInfo
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
