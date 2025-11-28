package com.unam.integrador.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.unam.integrador.dto.FacturacionMasivaDTO;
import com.unam.integrador.model.LoteFacturacion;
import com.unam.integrador.services.FacturacionMasivaService;

import jakarta.validation.Valid;

/**
 * Controlador web para la gestión de facturación masiva.
 * Maneja la interfaz HTML para ejecutar y consultar lotes de facturación.
 */
@Controller
@RequestMapping("/facturacion-masiva")
public class FacturacionMasivaController {
    
    @Autowired
    private FacturacionMasivaService facturacionMasivaService;
    
    /**
     * Muestra la lista de todos los lotes de facturación masiva.
     */
    @GetMapping
    public String listarLotes(Model model) {
        model.addAttribute("lotes", facturacionMasivaService.obtenerTodosLosLotes());
        return "facturacion-masiva/lista";
    }
    
    /**
     * Muestra el formulario para ejecutar una nueva facturación masiva.
     */
    @GetMapping("/nuevo")
    public String mostrarFormulario(Model model) {
        model.addAttribute("facturacionDTO", new FacturacionMasivaDTO());
        return "facturacion-masiva/formulario";
    }
    
    /**
     * Procesa el formulario y ejecuta la facturación masiva.
     */
    @PostMapping("/nuevo")
    public String ejecutarFacturacionMasiva(
            @Valid FacturacionMasivaDTO facturacionDTO,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        // Validar errores del formulario
        if (bindingResult.hasErrors()) {
            return "facturacion-masiva/formulario";
        }
        
        try {
            LoteFacturacion lote = facturacionMasivaService.ejecutarFacturacionMasiva(
                facturacionDTO.getPeriodo(),
                facturacionDTO.getFechaVencimiento(),
                facturacionDTO.getUsuario()
            );
            
            redirectAttributes.addFlashAttribute("mensaje", 
                String.format("Facturación masiva ejecutada exitosamente. " +
                    "Se generaron %d facturas por un total de $%.2f",
                    lote.getCantidadFacturas(),
                    lote.getMontoTotal())
            );
            
            return "redirect:/facturacion-masiva/" + lote.getId();
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("facturacionDTO", facturacionDTO);
            return "facturacion-masiva/formulario";
        }
    }
    
    /**
     * Muestra el detalle de un lote de facturación.
     */
    @GetMapping("/{id}")
    public String verDetalle(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            LoteFacturacion lote = facturacionMasivaService.obtenerLotePorId(id);
            model.addAttribute("lote", lote);
            return "facturacion-masiva/detalle";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/facturacion-masiva";
        }
    }
    
    /**
     * Muestra el formulario de confirmación para anular un lote.
     */
    @GetMapping("/{id}/confirmar-anulacion")
    public String confirmarAnulacion(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            LoteFacturacion lote = facturacionMasivaService.obtenerLotePorId(id);
            
            // Verificar que el lote puede ser anulado
            if (!lote.puedeSerAnulado()) {
                String mensaje = lote.isAnulado() 
                    ? "El lote ya está anulado."
                    : "No se puede anular el lote porque tiene " + 
                      lote.getCantidadFacturasConPagos() + " factura(s) con pagos.";
                redirectAttributes.addFlashAttribute("error", mensaje);
                return "redirect:/facturacion-masiva/" + id;
            }
            
            model.addAttribute("lote", lote);
            return "facturacion-masiva/confirmar-anulacion";
            
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/facturacion-masiva";
        }
    }
    
    /**
     * Procesa la anulación del lote de facturación.
     */
    @PostMapping("/{id}/anular")
    public String anularLote(
            @PathVariable Long id,
            @RequestParam String motivo,
            @RequestParam String usuario,
            RedirectAttributes redirectAttributes) {
        
        try {
            LoteFacturacion lote = facturacionMasivaService.anularLote(id, motivo, usuario);
            
            redirectAttributes.addFlashAttribute("mensaje", 
                String.format("Lote #%d anulado exitosamente. Se anularon %d facturas.",
                    lote.getId(),
                    lote.getCantidadFacturas())
            );
            
            return "redirect:/facturacion-masiva/" + id;
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/facturacion-masiva/" + id;
        }
    }
}
