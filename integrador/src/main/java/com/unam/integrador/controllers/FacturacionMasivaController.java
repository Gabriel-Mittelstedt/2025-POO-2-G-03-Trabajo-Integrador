package com.unam.integrador.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.unam.integrador.dto.FacturacionMasivaDTO;
import com.unam.integrador.model.LoteFacturacion;
import com.unam.integrador.services.FacturaService;

import jakarta.validation.Valid;

/**
 * Controlador web para la gestión de facturación masiva.
 * 
 * Implementa:
 * - HU-07: Emisión de facturación masiva por período
 * - HU-08: Anulación de facturación masiva
 * - HU-09: Consulta de facturación masiva
 */
@Controller
@RequestMapping("/facturacion-masiva")
public class FacturacionMasivaController {
    
    @Autowired
    private FacturaService facturaService;
    
    /**
     * Muestra la lista de lotes de facturación masiva.
     * Implementa HU-09: Consulta de facturación masiva.
     */
    @GetMapping
    public String listarLotes(Model model) {
        model.addAttribute("lotes", facturaService.listarLotesFacturacion());
        return "facturacion-masiva/lista";
    }
    
    /**
     * Muestra el formulario para crear una nueva facturación masiva.
     * Implementa HU-07: Emisión de facturación masiva por período.
     */
    @GetMapping("/nuevo")
    public String mostrarFormulario(Model model) {
        model.addAttribute("facturacionDTO", new FacturacionMasivaDTO());
        return "facturacion-masiva/formulario";
    }
    
    /**
     * Procesa el formulario de facturación masiva.
     * Genera facturas para todos los clientes activos con servicios contratados.
     * 
     * @param dto Datos del formulario
     * @param result Resultado de la validación
     * @param redirectAttributes Atributos para redirección
     * @param model Modelo para la vista
     * @return Redirección al detalle del lote o al formulario si hay errores
     */
    @PostMapping("/nuevo")
    public String ejecutarFacturacionMasiva(
            @Valid @ModelAttribute("facturacionDTO") FacturacionMasivaDTO dto,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        if (result.hasErrors()) {
            return "facturacion-masiva/formulario";
        }
        
        try {
            LoteFacturacion lote = facturaService.ejecutarFacturacionMasiva(
                dto.getPeriodo(),
                dto.getFechaVencimiento(),
                dto.getUsuario()
            );
            
            redirectAttributes.addFlashAttribute("mensaje", 
                String.format("Facturación masiva ejecutada exitosamente. " +
                    "Se generaron %d facturas por un monto total de $%.2f",
                    lote.getCantidadFacturas(),
                    lote.getMontoTotal())
            );
            
            return "redirect:/facturacion-masiva/" + lote.getId();
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            return "facturacion-masiva/formulario";
        }
    }
    
    /**
     * Muestra el detalle de un lote de facturación masiva.
     * Implementa HU-09: Consulta de facturación masiva.
     * 
     * @param id ID del lote
     * @param model Modelo para la vista
     * @return Vista de detalle
     */
    @GetMapping("/{id}")
    public String verDetalle(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            LoteFacturacion lote = facturaService.obtenerLoteConFacturas(id);
            model.addAttribute("lote", lote);
            return "facturacion-masiva/detalle";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/facturacion-masiva";
        }
    }
    
    /**
     * Muestra el formulario de confirmación para anular un lote.
     * Implementa HU-08: Anulación de facturación masiva.
     * 
     * @param id ID del lote
     * @param model Modelo para la vista
     * @return Vista de confirmación
     */
    @GetMapping("/{id}/confirmar-anulacion")
    public String confirmarAnulacion(@PathVariable Long id, Model model, 
                                     RedirectAttributes redirectAttributes) {
        try {
            LoteFacturacion lote = facturaService.obtenerLoteConFacturas(id);
            
            // Validar que el lote puede ser anulado
            if (!lote.puedeSerAnulado()) {
                redirectAttributes.addFlashAttribute("error", 
                    "No se puede anular el lote. Algunas facturas ya tienen pagos registrados.");
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
     * Procesa la anulación de un lote de facturación.
     * Genera notas de crédito para todas las facturas del lote.
     * 
     * @param id ID del lote
     * @param motivo Motivo de la anulación
     * @param usuario Usuario que ejecuta la anulación
     * @param redirectAttributes Atributos para redirección
     * @return Redirección al detalle del lote
     */
    @PostMapping("/{id}/anular")
    public String anularLote(
            @PathVariable Long id,
            @RequestParam String motivo,
            @RequestParam String usuario,
            RedirectAttributes redirectAttributes) {
        
        try {
            LoteFacturacion lote = facturaService.anularLoteFacturacion(id, motivo, usuario);
            
            redirectAttributes.addFlashAttribute("mensaje", 
                String.format("Lote #%d anulado exitosamente. " +
                    "Se generaron %d notas de crédito.",
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
