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
    
    /**
     * HU-18: Muestra el formulario para editar un servicio existente.
     * @param id ID del servicio a editar
     * @param model Modelo para la vista
     * @param redirectAttributes Atributos para redirección
     * @return Vista del formulario
     */
    @GetMapping("/{id}/editar")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model,
                                          RedirectAttributes redirectAttributes) {
        try {
            Servicio servicio = servicioService.buscarPorId(id);
            model.addAttribute("servicio", servicio);
            model.addAttribute("alicuotas", TipoAlicuotaIVA.values());
            model.addAttribute("accion", "Editar");
            return "servicios/formulario";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/servicios";
        }
    }
    
    /**
     * HU-18: Procesa el formulario para modificar un servicio.
     * @param id ID del servicio a modificar
     * @param servicio Datos actualizados del servicio
     * @param redirectAttributes Atributos para redirección
     * @return Redirección al detalle del servicio
     */
    @PostMapping("/{id}/editar")
    public String guardarEdicion(@PathVariable Long id,
                                @ModelAttribute Servicio servicio,
                                RedirectAttributes redirectAttributes) {
        try {
            Servicio servicioModificado = servicioService.modificarServicio(id, servicio);
            redirectAttributes.addFlashAttribute("mensaje", 
                "Servicio '" + servicioModificado.getNombre() + "' modificado exitosamente");
            redirectAttributes.addFlashAttribute("tipoMensaje", "success");
            return "redirect:/servicios/" + id;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/servicios/" + id + "/editar";
        }
    }
    
    /**
     * HU-19: Muestra la confirmación para dar de baja un servicio.
     * @param id ID del servicio a dar de baja
     * @param model Modelo para la vista
     * @param redirectAttributes Atributos para redirección
     * @return Vista de confirmación
     */
    @GetMapping("/{id}/confirmar-eliminar")
    public String confirmarEliminar(@PathVariable Long id, Model model,
                                    RedirectAttributes redirectAttributes) {
        try {
            Servicio servicio = servicioService.buscarPorId(id);
            model.addAttribute("servicio", servicio);
            return "servicios/confirmar-eliminar";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/servicios";
        }
    }
    
    /**
     * HU-19: Procesa la baja de un servicio.
     * @param id ID del servicio a dar de baja
     * @param redirectAttributes Atributos para redirección
     * @return Redirección al listado
     */
    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, 
                          RedirectAttributes redirectAttributes) {
        try {
            Servicio servicio = servicioService.darDeBajaServicio(id);
            redirectAttributes.addFlashAttribute("mensaje", 
                "Servicio '" + servicio.getNombre() + "' dado de baja exitosamente");
            redirectAttributes.addFlashAttribute("tipoMensaje", "warning");
            return "redirect:/servicios";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/servicios";
        }
    }
}
