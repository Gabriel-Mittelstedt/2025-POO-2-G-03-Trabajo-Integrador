package com.unam.integrador.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.unam.integrador.model.Servicio;
import com.unam.integrador.model.enums.TipoAlicuotaIVA;
import com.unam.integrador.services.ServicioService;

/**
 * Controlador para gestionar operaciones sobre Servicios.
 */
@Controller
@RequestMapping("/servicios")
public class ServicioController {
    
    @Autowired
    private ServicioService servicioService;
    
    /**
     * HU-17: Listado de servicios con búsqueda opcional.
     * @param busqueda Término de búsqueda opcional
     * @param model Modelo para la vista
     * @return Vista del listado
     */
    @GetMapping
    public String listar(@RequestParam(required = false) String busqueda, Model model) {
        List<Servicio> servicios;
        
        if (busqueda != null && !busqueda.trim().isEmpty()) {
            servicios = servicioService.buscar(busqueda);
            model.addAttribute("busqueda", busqueda);
        } else {
            servicios = servicioService.listarTodos();
        }
        
        model.addAttribute("servicios", servicios);
        return "servicios/lista";
    }
    
    /**
     * HU-16: Muestra el formulario para crear un nuevo servicio.
     * @param model Modelo para la vista
     * @return Vista del formulario
     */
    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(Model model) {
        model.addAttribute("servicio", new Servicio());
        model.addAttribute("alicuotas", TipoAlicuotaIVA.values());
        model.addAttribute("accion", "Crear");
        return "servicios/formulario";
    }
    
    /**
     * HU-16: Procesa el formulario para crear un nuevo servicio.
     * @param servicio Datos del servicio
     * @param redirectAttributes Atributos para redirección
     * @return Redirección al listado
     */
    @PostMapping("/nuevo")
    public String guardarNuevo(@ModelAttribute Servicio servicio,
                               RedirectAttributes redirectAttributes) {
        try {
            servicioService.crearServicio(servicio);
            redirectAttributes.addFlashAttribute("mensaje", 
                "Servicio '" + servicio.getNombre() + "' creado exitosamente");
            redirectAttributes.addFlashAttribute("tipoMensaje", "success");
            return "redirect:/servicios";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/servicios/nuevo";
        }
    }
    
    /**
     * Ver detalle de un servicio.
     * @param id ID del servicio
     * @param model Modelo para la vista
     * @return Vista de detalle
     */
    @GetMapping("/{id}")
    public String verDetalle(@PathVariable Long id, Model model) {
        try {
            Servicio servicio = servicioService.buscarPorId(id);
            model.addAttribute("servicio", servicio);
            return "servicios/detalle";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/servicios";
        }
    }
}
