package com.unam.integrador.controllers;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.unam.integrador.model.Factura;
import com.unam.integrador.services.CuentaClienteService;
import com.unam.integrador.services.FacturaService;

/**
 * Controlador web para la gestión de facturas individuales.
 * Maneja la interfaz HTML para emisión y consulta de facturas.
 */
@Controller
@RequestMapping("/facturas")
public class FacturaViewController {
    
    @Autowired
    private FacturaService facturaService;
    
    @Autowired
    private CuentaClienteService clienteService;
    
    /**
     * Muestra la lista de todas las facturas.
     */
    @GetMapping
    public String listarFacturas(Model model) {
        model.addAttribute("facturas", facturaService.listarTodasLasFacturas());
        return "facturas/lista";
    }
    
    /**
     * Muestra el formulario para emitir una factura individual.
     * La factura se generará automáticamente desde los servicios contratados del cliente.
     */
    @GetMapping("/nueva-individual")
    public String mostrarFormularioFacturaIndividual(Model model) {
        // Cargar lista de clientes activos para el select
        model.addAttribute("clientes", clienteService.obtenerTodosLosClientes());
        model.addAttribute("fechaEmision", LocalDate.now());
        return "facturas/formulario-individual";
    }
    
    /**
     * Procesa el formulario de emisión de factura individual.
     * Los items se generan automáticamente desde los servicios contratados del cliente.
     * 
     * @param clienteId ID del cliente
     * @param periodo Período de facturación (formato YYYYMM)
     * @param fechaEmision Fecha de emisión
     * @param fechaVencimiento Fecha de vencimiento
     * @param porcentajeDescuento Descuento opcional
     * @param motivoDescuento Motivo del descuento
     */
    @PostMapping("/nueva-individual")
    public String emitirFacturaIndividual(
            @RequestParam Long clienteId,
            @RequestParam String periodo,
            @RequestParam LocalDate fechaEmision,
            @RequestParam LocalDate fechaVencimiento,
            @RequestParam(required = false) Double porcentajeDescuento,
            @RequestParam(required = false) String motivoDescuento,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        try {
            // Emitir factura usando servicios contratados
            Factura factura = facturaService.emitirFacturaDesdeServiciosContratados(
                clienteId, 
                periodo, 
                fechaEmision,
                fechaVencimiento, 
                porcentajeDescuento, 
                motivoDescuento
            );
            
            redirectAttributes.addFlashAttribute("success", 
                String.format("Factura %04d-%08d emitida exitosamente. Total: $%.2f", 
                    factura.getSerie(), 
                    factura.getNroFactura(), 
                    factura.getTotal())
            );
            return "redirect:/facturas/" + factura.getIdFactura();
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("clientes", clienteService.obtenerTodosLosClientes());
            model.addAttribute("clienteId", clienteId);
            model.addAttribute("periodo", periodo);
            model.addAttribute("fechaEmision", fechaEmision);
            model.addAttribute("fechaVencimiento", fechaVencimiento);
            model.addAttribute("porcentajeDescuento", porcentajeDescuento);
            model.addAttribute("motivoDescuento", motivoDescuento);
            return "facturas/formulario-individual";
        }
    }
    
    /**
     * Muestra el detalle de una factura específica.
     */
    @GetMapping("/{id}")
    public String verDetalle(@PathVariable Long id, Model model) {
        Factura factura = facturaService.obtenerFacturaPorId(id);
        model.addAttribute("factura", factura);
        return "facturas/detalle";
    }
    
    /**
     * Lista facturas de un cliente específico.
     */
    @GetMapping("/cliente/{clienteId}")
    public String listarFacturasPorCliente(@PathVariable Long clienteId, Model model) {
        model.addAttribute("facturas", facturaService.listarFacturasPorCliente(clienteId));
        model.addAttribute("cliente", clienteService.obtenerClientePorId(clienteId));
        return "facturas/lista";
    }
    
    /**
     * Lista facturas de un período específico.
     */
    @GetMapping("/periodo/{periodo}")
    public String listarFacturasPorPeriodo(@PathVariable String periodo, Model model) {
        model.addAttribute("facturas", facturaService.listarFacturasPorPeriodo(periodo));
        model.addAttribute("periodo", periodo);
        return "facturas/lista";
    }
}
