package com.unam.integrador.services;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unam.integrador.model.Factura;
import com.unam.integrador.model.Pago;
import com.unam.integrador.model.Recibo;
import com.unam.integrador.model.enums.MetodoPago;
import com.unam.integrador.repositories.FacturaRepository;
import com.unam.integrador.repositories.PagoRepository;
import com.unam.integrador.repositories.ReciboRepository;

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
