package com.unam.integrador.controllers;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.unam.integrador.model.CuentaCliente;
import com.unam.integrador.model.Factura;
import com.unam.integrador.model.Pago;
import com.unam.integrador.model.Recibo;
import com.unam.integrador.model.enums.MetodoPago;
import com.unam.integrador.services.CuentaClienteService;
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
    
    @Autowired
    private CuentaClienteService cuentaClienteService;
    
    /**
     * Muestra la lista de todos los pagos.
     */
    @GetMapping
    public String listarPagos(Model model) {
        model.addAttribute("pagos", pagoService.listarTodos());
        return "pagos/lista";
    }

    /**
     * Muestra un formulario simple para buscar cliente por nombre.
     */
    /**
     * Procesa el formulario de búsqueda por nombre y redirige a la pantalla
     * de facturas mostrando la ventana emergente. No existe una página
     * independiente para buscar cliente: toda la interacción ocurre en el
     * modal dentro de `/facturas`.
     */
    @PostMapping("/buscar-cliente")
    public String procesarBusquedaCliente(@RequestParam("clienteNombre") String clienteNombre,
                                          RedirectAttributes redirectAttributes) {
        if (clienteNombre == null || clienteNombre.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("modalSearchError", "El nombre del cliente es obligatorio");
            return "redirect:/facturas#buscar-cliente-modal";
        }

        java.util.List<CuentaCliente> clientes = cuentaClienteService.buscarPorNombre(clienteNombre.trim());
        if (clientes.isEmpty()) {
            redirectAttributes.addFlashAttribute("modalSearchError", "No se encontraron clientes con ese nombre");
            redirectAttributes.addFlashAttribute("clienteNombre", clienteNombre.trim());
            return "redirect:/facturas#buscar-cliente-modal";
        }
        if (clientes.size() == 1) {
            // Redirigir directamente a la pantalla de selección de facturas
            return "redirect:/pagos/seleccionar-facturas/" + clientes.get(0).getId();
        }

        // Si hay múltiples coincidencias, redirigir a la lista de facturas
        // y abrir la ventana modal con las coincidencias (usando flash attributes)
        redirectAttributes.addFlashAttribute("modalClients", clientes);
        redirectAttributes.addFlashAttribute("clienteNombre", clienteNombre.trim());
        return "redirect:/facturas#buscar-cliente-modal";
    }

    /**
     * Muestra la pantalla de selección de facturas impagas para un cliente.
     * (UI-only: permite seleccionar varias facturas, sin procesar el pago)
     */
    @GetMapping("/seleccionar-facturas/{clienteId}")
    public String mostrarSeleccionFacturas(@PathVariable Long clienteId,
                                           @RequestParam(value = "facturaId", required = false) Long facturaId,
                                           Model model) {
        CuentaCliente cliente = cuentaClienteService.obtenerClientePorId(clienteId);
        java.util.List<Factura> facturasImpagas = pagoService.listarFacturasImpagasPorCliente(clienteId);
        model.addAttribute("cliente", cliente);
        model.addAttribute("facturas", facturasImpagas);
        // Agregar métodos de pago disponibles y total adeudado para prellenar el formulario
        model.addAttribute("metodosPago", MetodoPago.values());
        java.math.BigDecimal totalAdeudado = facturasImpagas.stream()
                .map(Factura::getSaldoPendiente)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        model.addAttribute("totalAdeudado", totalAdeudado);
        // Si se pasó facturaId, marcarla como preseleccionada en la vista
        model.addAttribute("preselectedFacturaId", facturaId);
        // Si se pasó facturaId, usar su saldoPendiente para prellenar el monto sugerido
        if (facturaId != null) {
            try {
                Factura facturaSeleccionada = facturaService.obtenerFacturaPorId(facturaId);
                if (facturaSeleccionada != null && facturaSeleccionada.getSaldoPendiente() != null) {
                    model.addAttribute("suggestedMonto", facturaSeleccionada.getSaldoPendiente());
                }
            } catch (Exception e) {
                // no interrumpir el flujo si no se encuentra la factura
            }
        }
        return "pagos/seleccionar-facturas";
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
    
    // Nota: Los endpoints y vistas de pago total/parcial fueron removidos
    // en favor del flujo unificado de Pago Combinado. Las operaciones
    // siguen disponibles vía POST a /pagos/registrar-combinado usando una
    // lista de `facturasIds`.
    
    /**
     * Muestra el detalle de un pago.
     */
    @GetMapping("/{id}")
    public String verDetalle(@PathVariable Long id, Model model) {
        Pago pago = pagoService.buscarPorId(id);
        model.addAttribute("pago", pago);
        return "pagos/detalle";
    }
    
    /**
     * Muestra el formulario para registrar un pago combinado (múltiples facturas).
     */
    @GetMapping("/nuevo-combinado/{clienteId}")
    public String mostrarFormularioPagoCombinado(@PathVariable Long clienteId, Model model) {
        // Obtener el cliente
        CuentaCliente cliente = cuentaClienteService.obtenerClientePorId(clienteId);
        
        // Obtener facturas impagas del cliente
        List<Factura> facturasImpagas = pagoService.listarFacturasImpagasPorCliente(clienteId);
        
        if (facturasImpagas.isEmpty()) {
            model.addAttribute("error", "El cliente no tiene facturas impagas para pagar");
        }
        
        // Calcular el total adeudado
        BigDecimal totalAdeudado = facturasImpagas.stream()
            .map(Factura::getSaldoPendiente)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        model.addAttribute("cliente", cliente);
        model.addAttribute("facturas", facturasImpagas);
        model.addAttribute("totalAdeudado", totalAdeudado);
        model.addAttribute("metodosPago", MetodoPago.values());
        
        return "pagos/formulario-combinado";
    }
    
    /**
     * Procesa el formulario de pago combinado.
     * Recibe los parámetros directamente del formulario HTML sin usar DTO.
     */
    @PostMapping("/registrar-combinado")
    public String registrarPagoCombinado(
            @RequestParam(value = "facturasIds", required = false) List<Long> facturasIds,
            @RequestParam(value = "montoTotal", required = false) BigDecimal montoTotal,
            @RequestParam("metodoPago") MetodoPago metodoPago,
            @RequestParam(value = "referencia", required = false) String referencia,
            @RequestParam(value = "clienteId", required = false) Long clienteId,
            RedirectAttributes redirectAttributes) {
        // Validación: debe seleccionarse al menos una factura
        if (facturasIds == null || facturasIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Seleccione al menos una factura para registrar el pago");
            if (montoTotal != null) {
                redirectAttributes.addFlashAttribute("suggestedMonto", montoTotal);
            }
            if (clienteId != null) {
                return "redirect:/pagos/seleccionar-facturas/" + clienteId;
            }
            // Si no tenemos cliente, volver al listado de facturas
            return "redirect:/facturas";
        }

        try {
            // Si no se proporcionó montoTotal, calcularlo como la suma de saldos pendientes
            if (montoTotal == null) {
                java.math.BigDecimal suma = java.math.BigDecimal.ZERO;
                for (Long id : facturasIds) {
                    try {
                        Factura f = facturaService.obtenerFacturaPorId(id);
                        if (f != null && f.getSaldoPendiente() != null) {
                            suma = suma.add(f.getSaldoPendiente());
                        }
                    } catch (Exception ex) {
                        // ignorar facturas no encontradas en el cálculo
                    }
                }
                montoTotal = suma;
            }

            // Llamar al servicio que orquesta las entidades de dominio
            Recibo recibo = pagoService.registrarPagoCombinado(
                facturasIds,
                montoTotal,
                metodoPago,
                referencia
            );

            redirectAttributes.addFlashAttribute("mensaje",
                "Pago combinado registrado exitosamente. Recibo N° " + recibo.getNumero() + " generado.");
            return "redirect:/pagos";
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            if (clienteId != null) {
                return "redirect:/pagos/seleccionar-facturas/" + clienteId;
            }
            return "redirect:/pagos";
        }
    }
}
