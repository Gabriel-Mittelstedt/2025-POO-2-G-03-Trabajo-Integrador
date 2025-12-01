package com.unam.integrador.controllers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.unam.integrador.dto.ReciboDTO;
import com.unam.integrador.model.CuentaCliente;
import com.unam.integrador.model.Factura;
import com.unam.integrador.model.Pago;
import com.unam.integrador.model.enums.MetodoPago;
import com.unam.integrador.services.CuentaClienteService;
import com.unam.integrador.services.FacturaService;
import com.unam.integrador.services.PagoService;
import com.unam.integrador.services.ReciboService;

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
    
    @Autowired
    private ReciboService reciboService;
    
    /**
     * Muestra la lista de todos los pagos.
     */
    @GetMapping
    public String listarPagos(
            @RequestParam(value = "clienteNombre", required = false) String clienteNombre,
            @RequestParam(value = "desde", required = false) String desdeStr,
            @RequestParam(value = "hasta", required = false) String hastaStr,
            Model model) {

        LocalDate desde = null;
        LocalDate hasta = null;
        try {
            if (desdeStr != null && !desdeStr.isBlank()) {
                desde = LocalDate.parse(desdeStr);
            }
        } catch (DateTimeParseException e) {
            model.addAttribute("error", "Fecha 'desde' inválida. Use YYYY-MM-DD.");
        }
        try {
            if (hastaStr != null && !hastaStr.isBlank()) {
                hasta = LocalDate.parse(hastaStr);
            }
        } catch (DateTimeParseException e) {
            model.addAttribute("error", "Fecha 'hasta' inválida. Use YYYY-MM-DD.");
        }

        model.addAttribute("clienteNombre", clienteNombre);
        model.addAttribute("desde", desdeStr);
        model.addAttribute("hasta", hastaStr);

        // Usar ReciboService para generar recibos dinámicamente desde los pagos
        List<Pago> pagos = pagoService.listarFiltrados(clienteNombre, desde, hasta);
        
        // Agrupar pagos por número de recibo para evitar duplicados
        Map<String, List<Pago>> pagosPorRecibo = new java.util.HashMap<>();
        for (Pago pago : pagos) {
            String clave = (pago.getNumeroRecibo() != null) ? pago.getNumeroRecibo() : String.valueOf(pago.getIDPago());
            if (!pagosPorRecibo.containsKey(clave)) {
                pagosPorRecibo.put(clave, new java.util.ArrayList<>());
            }
            pagosPorRecibo.get(clave).add(pago);
        }
        
        // Generar un ReciboDTO por cada grupo de pagos con el mismo número de recibo
        List<ReciboDTO> recibos = new java.util.ArrayList<>();
        for (List<Pago> gruposPagos : pagosPorRecibo.values()) {
            ReciboDTO recibo;
            if (gruposPagos.size() == 1) {
                recibo = reciboService.generarReciboDesdePago(gruposPagos.get(0));
            } else {
                String numeroRecibo = gruposPagos.get(0).getNumeroRecibo();
                recibo = reciboService.generarReciboDesdeMultiplesPagos(gruposPagos, numeroRecibo);
            }
            recibos.add(recibo);
        }
        
        // Ordenar por número de recibo descendente
        recibos.sort((r1, r2) -> r2.getNumero().compareTo(r1.getNumero()));

        model.addAttribute("recibos", recibos);
        return "pagos/lista";
    }

    @GetMapping("/recibo/{id}")
    public String verReciboDetalle(@PathVariable Long id, Model model) {
        // Generar el ReciboDTO dinámicamente desde el Pago
        ReciboDTO recibo = reciboService.generarReciboPorPagoId(id);
        model.addAttribute("recibo", recibo);
        return "pagos/recibo-detalle";
    }
    
    @GetMapping("/recibo/numero/{numero}")
    public String verReciboDetalleConsolidado(@PathVariable String numero, Model model) {
        // Generar el ReciboDTO consolidado desde múltiples pagos con el mismo número de recibo
        ReciboDTO recibo = reciboService.generarReciboConsolidado(numero);
        model.addAttribute("recibo", recibo);
        return "pagos/recibo-detalle";
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
        // Mostrar coincidencias en el modal (incluso si sólo hay una) para
        // que el usuario pueda seleccionar explícitamente el cliente.
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
        // Agregar métodos de pago disponibles (sin SALDO_A_FAVOR) y total adeudado para prellenar el formulario
        List<MetodoPago> metodosPagoDisponibles = new java.util.ArrayList<>();
        for (MetodoPago m : MetodoPago.values()) {
            if (m != MetodoPago.SALDO_A_FAVOR) {
                metodosPagoDisponibles.add(m);
            }
        }
        model.addAttribute("metodosPago", metodosPagoDisponibles.toArray(new MetodoPago[0]));
        
        java.math.BigDecimal totalAdeudado = java.math.BigDecimal.ZERO;
        for (Factura factura : facturasImpagas) {
            totalAdeudado = totalAdeudado.add(factura.getSaldoPendiente());
        }
        model.addAttribute("totalAdeudado", totalAdeudado);
        // Calcular el máximo de saldo a favor que puede aplicarse: no debe exceder
        // ni el saldo disponible del cliente ni el total adeudado de las facturas
        java.math.BigDecimal maxSaldoAplicable = java.math.BigDecimal.ZERO;
        if (cliente != null && cliente.tieneSaldoAFavor()) {
            java.math.BigDecimal saldoAFavor = cliente.getSaldoAFavor();
            maxSaldoAplicable = saldoAFavor.min(totalAdeudado);
        }
        model.addAttribute("maxSaldoAplicable", maxSaldoAplicable);
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
        // Redirigir al flujo unificado de Registrar Pago (selección de facturas)
        try {
            Factura factura = facturaService.obtenerFacturaPorId(facturaId);
            if (factura != null && factura.getCliente() != null) {
                Long clienteId = factura.getCliente().getId();
                return "redirect:/pagos/seleccionar-facturas/" + clienteId + "?facturaId=" + facturaId;
            }
        } catch (Exception e) {
            // Si hay algún problema, volver al detalle de la factura
        }
        return "redirect:/facturas/" + facturaId;
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
        BigDecimal totalAdeudado = BigDecimal.ZERO;
        for (Factura factura : facturasImpagas) {
            totalAdeudado = totalAdeudado.add(factura.getSaldoPendiente());
        }
        
        model.addAttribute("cliente", cliente);
        model.addAttribute("facturas", facturasImpagas);
        model.addAttribute("totalAdeudado", totalAdeudado);
        
        List<MetodoPago> metodosPagoDisponibles = new java.util.ArrayList<>();
        for (MetodoPago m : MetodoPago.values()) {
            if (m != MetodoPago.SALDO_A_FAVOR) {
                metodosPagoDisponibles.add(m);
            }
        }
        model.addAttribute("metodosPago", metodosPagoDisponibles.toArray(new MetodoPago[0]));
        
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
            @RequestParam(value = "saldoAFavorAplicar", required = false) BigDecimal saldoAFavorAplicar,
            @RequestParam(value = "metodoPago", required = false) MetodoPago metodoPago,
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
            // Si no se proporcionó saldoAFavorAplicar, usar 0
            if (saldoAFavorAplicar == null) {
                saldoAFavorAplicar = BigDecimal.ZERO;
            }
            
            // Si no se proporcionó montoTotal, usar 0 (solo se aplicará saldo a favor)
            if (montoTotal == null) {
                montoTotal = BigDecimal.ZERO;
            }
            
            // Si no se proporcionó metodoPago y hay monto, es un error
            if (metodoPago == null && montoTotal.compareTo(BigDecimal.ZERO) > 0) {
                redirectAttributes.addFlashAttribute("error", "Debe seleccionar un método de pago cuando ingresa un monto");
                if (clienteId != null) {
                    return "redirect:/pagos/seleccionar-facturas/" + clienteId;
                }
                return "redirect:/pagos";
            }
            
            // Si no hay metodoPago, usar SALDO_A_FAVOR por defecto
            if (metodoPago == null) {
                metodoPago = MetodoPago.SALDO_A_FAVOR;
            }

            // Llamar al servicio que orquesta las entidades de dominio
            String numeroRecibo = pagoService.registrarPagoCombinado(
                facturasIds,
                montoTotal,
                saldoAFavorAplicar,
                metodoPago,
                referencia
            );

            redirectAttributes.addFlashAttribute("mensaje",
                "Pago combinado registrado exitosamente. Recibo N° " + numeroRecibo + " generado.");
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