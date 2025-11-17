package com.unam.integrador.controllers;

import java.math.BigDecimal;

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
import com.unam.integrador.model.Pago;
import com.unam.integrador.model.enums.MetodoPago;
import com.unam.integrador.services.FacturaService;
import com.unam.integrador.services.PagoService;

/**
 * Controlador web para la gestión de pagos.
 * Maneja la interfaz HTML para registro de pagos.
 */
@Controller
@RequestMapping("/pagos")
public class PagoController {
    
    @Autowired
    private PagoService pagoService;
    
    @Autowired
    private FacturaService facturaService;
    
    /**
     * Muestra la lista de todos los pagos.
     */
    @GetMapping
    public String listarPagos(Model model) {
        model.addAttribute("pagos", pagoService.listarTodos());
        return "pagos/lista";
    }
    
    /**
     * Muestra el formulario para registrar un pago total de una factura.
     */
    @GetMapping("/nuevo-total/{facturaId}")
    public String mostrarFormularioPagoTotal(@PathVariable Long facturaId, Model model) {
        Factura factura = facturaService.obtenerFacturaPorId(facturaId);
        model.addAttribute("factura", factura);
        model.addAttribute("metodosPago", MetodoPago.values());
        return "pagos/formulario-total";
    }
    
    /**
     * Procesa el formulario de registro de pago total.
     */
    @PostMapping("/registrar-total")
    public String registrarPagoTotal(
            @RequestParam Long facturaId,
            @RequestParam MetodoPago metodoPago,
            @RequestParam(required = false) String referencia,
            RedirectAttributes redirectAttributes) {
        try {
            Pago pago = pagoService.registrarPagoTotal(facturaId, metodoPago, referencia);
            redirectAttributes.addFlashAttribute("mensaje", 
                "Pago registrado exitosamente. Recibo generado.");
            return "redirect:/pagos/" + pago.getIDPago();
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/pagos/nuevo-total/" + facturaId;
        }
    }
    
    /**
     * Muestra el formulario para registrar un pago parcial de una factura.
     */
    @GetMapping("/nuevo-parcial/{facturaId}")
    public String mostrarFormularioPagoParcial(@PathVariable Long facturaId, Model model) {
        Factura factura = facturaService.obtenerFacturaPorId(facturaId);
        model.addAttribute("factura", factura);
        model.addAttribute("metodosPago", MetodoPago.values());
        return "pagos/formulario-parcial";
    }
    
    /**
     * Procesa el formulario de registro de pago parcial.
     */
    @PostMapping("/registrar-parcial")
    public String registrarPagoParcial(
            @RequestParam Long facturaId,
            @RequestParam BigDecimal monto,
            @RequestParam MetodoPago metodoPago,
            @RequestParam(required = false) String referencia,
            RedirectAttributes redirectAttributes) {
        try {
            Pago pago = pagoService.registrarPagoParcial(facturaId, monto, metodoPago, referencia);
            redirectAttributes.addFlashAttribute("mensaje", 
                "Pago parcial registrado exitosamente. Recibo generado.");
            return "redirect:/pagos/" + pago.getIDPago();
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/pagos/nuevo-parcial/" + facturaId;
        }
    }
    
    /**
     * Muestra el detalle de un pago.
     */
    @GetMapping("/{id}")
    public String verDetalle(@PathVariable Long id, Model model) {
        Pago pago = pagoService.buscarPorId(id);
        model.addAttribute("pago", pago);
        return "pagos/detalle";
    }
}
