package com.unam.integrador.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.unam.integrador.model.CuentaCliente;
import com.unam.integrador.model.enums.EstadoCuenta;
import com.unam.integrador.model.enums.TipoCondicionIVA;
import com.unam.integrador.services.CuentaClienteService;

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
    
}
