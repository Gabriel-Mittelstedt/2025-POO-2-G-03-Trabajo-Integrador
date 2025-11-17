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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.unam.integrador.model.CuentaCliente;
import com.unam.integrador.model.enums.EstadoCuenta;
import com.unam.integrador.model.enums.TipoCondicionIVA;
import com.unam.integrador.services.CuentaClienteService;
import com.unam.integrador.services.ServicioService;

import jakarta.validation.Valid;

/**
 * Controlador para la gestión de cuentas de clientes.
 * Maneja las operaciones CRUD y renderización de vistas.
 */
@Controller
@RequestMapping("/clientes")
public class CuentaClienteController {
    
    @Autowired
    private CuentaClienteService clienteService;
    
    @Autowired
    private ServicioService servicioService;
    
    /**
     * Muestra la lista de todos los clientes.
     */
    @GetMapping
    public String listarClientes(Model model) {
        model.addAttribute("clientes", clienteService.obtenerTodosLosClientes());
        return "clientes/lista";
    }
    
    /**
     * Muestra el formulario para crear un nuevo cliente.
     */
    @GetMapping("/nuevo")
    public String mostrarFormularioNuevoCliente(Model model) {
        model.addAttribute("cliente", new CuentaCliente());
        model.addAttribute("condicionesIva", TipoCondicionIVA.values());
        model.addAttribute("estadosCuenta", EstadoCuenta.values());
        return "clientes/formulario";
    }
    
    /**
     * Procesa el formulario de creación de cliente.
     * Bean Validation valida automáticamente @NotBlank, @Email, etc.
     */
    @PostMapping("/nuevo")
    public String crearCliente(@Valid @ModelAttribute("cliente") CuentaCliente cliente,
                              BindingResult result,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        // Si hay errores de validación, volver al formulario
        if (result.hasErrors()) {
            model.addAttribute("cliente", cliente);
            model.addAttribute("condicionesIva", TipoCondicionIVA.values());
            model.addAttribute("estadosCuenta", EstadoCuenta.values());
            return "clientes/formulario";
        }
        
        try {
            CuentaCliente clienteCreado = clienteService.crearCliente(cliente);
            redirectAttributes.addFlashAttribute("success", 
                "Cliente creado exitosamente: " + clienteCreado.getNombre());
            return "redirect:/clientes";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("cliente", cliente);
            model.addAttribute("condicionesIva", TipoCondicionIVA.values());
            model.addAttribute("estadosCuenta", EstadoCuenta.values());
            return "clientes/formulario";
        }
    }
    
    /**
     * Muestra el detalle de un cliente con sus servicios contratados.
     */
    @GetMapping("/{id}")
    public String verDetalle(@PathVariable Long id, Model model) {
        CuentaCliente cliente = clienteService.obtenerClientePorId(id);
        model.addAttribute("cliente", cliente);
        return "clientes/detalle";
    }
    
    /**
     * Muestra el formulario para asignar servicios a un cliente.
     * Solo muestra servicios que el cliente aún no tiene contratados.
     */
    @GetMapping("/{id}/servicios/asignar")
    public String mostrarFormularioAsignarServicio(@PathVariable Long id, Model model) {
        CuentaCliente cliente = clienteService.obtenerClientePorId(id);
        
        // Filtrar servicios que ya están contratados activamente
        var serviciosDisponibles = servicioService.listarActivos().stream()
            .filter(servicio -> !cliente.tieneServicioContratadoActivo(servicio))
            .toList();
        
        model.addAttribute("cliente", cliente);
        model.addAttribute("servicios", serviciosDisponibles);
        return "clientes/agregar-servicio";
    }
    
    /**
     * Procesa la asignación de un servicio a un cliente.
     */
    @PostMapping("/{clienteId}/servicios/{servicioId}/asignar")
    public String asignarServicio(@PathVariable Long clienteId,
                                  @PathVariable Long servicioId,
                                  RedirectAttributes redirectAttributes) {
        try {
            clienteService.asignarServicio(clienteId, servicioId);
            redirectAttributes.addFlashAttribute("mensaje", "Servicio asignado exitosamente");
            return "redirect:/clientes/" + clienteId;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/clientes/" + clienteId + "/servicios/asignar";
        }
    }
    
    /**
     * Muestra el histórico completo de servicios del cliente (activos e inactivos).
     */
    @GetMapping("/{id}/servicios/historico")
    public String verHistoricoServicios(@PathVariable Long id, Model model) {
        CuentaCliente cliente = clienteService.obtenerClientePorId(id);
        model.addAttribute("cliente", cliente);
        model.addAttribute("serviciosContratados", cliente.getServiciosContratados());
        return "clientes/historico-servicios";
    }
    
}
