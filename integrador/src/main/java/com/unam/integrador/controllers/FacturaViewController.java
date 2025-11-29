package com.unam.integrador.controllers;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
    public String listarFacturas(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String tipoFactura,
            @RequestParam(required = false) String periodo,
            Model model) {

        // Actualizar facturas vencidas antes de mostrar el listado
        facturaService.actualizarFacturasVencidas();

        // Proveer opciones para los selects en la vista
        model.addAttribute("estados", com.unam.integrador.model.enums.EstadoFactura.values());
        model.addAttribute("tipos", com.unam.integrador.model.enums.TipoFactura.values());

        model.addAttribute("facturas", facturaService.listarFacturasFiltradas(estado, tipoFactura, periodo));
        model.addAttribute("periodo", periodo);
        model.addAttribute("estado", estado);
        model.addAttribute("tipoFactura", tipoFactura);
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
        model.addAttribute("periodos", generarOpcionesPeriodos());
        return "facturas/formulario-individual";
    }
    
    /**
     * Genera la lista de períodos disponibles para facturación.
     * Incluye 2 meses hacia atrás, el mes actual y 12 meses hacia adelante.
     * 
     * @return Lista de strings con los períodos en formato "Mes Año"
     */
    private List<String> generarOpcionesPeriodos() {
        List<String> periodos = new ArrayList<>();
        YearMonth mesActual = YearMonth.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.of("es", "ES"));
        
        // 2 meses hacia atrás + mes actual + 12 meses hacia adelante = 15 períodos
        for (int i = -2; i <= 12; i++) {
            YearMonth mes = mesActual.plusMonths(i);
            // Capitalizar primera letra del mes
            String periodo = mes.format(formatter);
            periodo = periodo.substring(0, 1).toUpperCase() + periodo.substring(1);
            periodos.add(periodo);
        }
        
        return periodos;
    }

    /**
     * Convierte un string de período en formato "Mes Año" a LocalDate.
     * El día siempre será 1.
     * 
     * @param periodoStr String del período (ej: "Noviembre 2025")
     * @return LocalDate con el primer día del mes indicado
     */
    private LocalDate convertirPeriodoALocalDate(String periodoStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.of("es", "ES"));
        YearMonth yearMonth = YearMonth.parse(periodoStr.toLowerCase(), formatter);
        return yearMonth.atDay(1);
    }
    
    /**
     * Procesa el formulario de emisión de factura individual.
     * Los items se generan automáticamente desde los servicios contratados del cliente.
     * 
     * @param clienteId ID del cliente
     * @param periodoStr Período de facturación (formato "Mes Año")
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
            // Convertir el período de String a LocalDate
            LocalDate periodoDate = convertirPeriodoALocalDate(periodo);
            
            // Emitir factura usando servicios contratados
            Factura factura = facturaService.emitirFacturaDesdeServiciosContratados(
                clienteId, 
                periodoDate, 
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
            model.addAttribute("periodos", generarOpcionesPeriodos());
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
        // Actualizar facturas vencidas antes de mostrar el detalle
        facturaService.actualizarFacturasVencidas();
        
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
     * El período viene en formato "Mes Año" y se convierte a LocalDate.
     */
    @GetMapping("/periodo/{periodo}")
    public String listarFacturasPorPeriodo(@PathVariable String periodo, Model model) {
        LocalDate periodoDate = convertirPeriodoALocalDate(periodo);
        model.addAttribute("facturas", facturaService.listarFacturasPorPeriodo(periodoDate));
        model.addAttribute("periodo", periodo);
        return "facturas/lista";
    }

    /**
     * Muestra el formulario de confirmación para anular una factura.
     */
    @GetMapping("/{id}/confirmar-anulacion")
    public String confirmarAnulacion(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Factura factura = facturaService.obtenerFacturaPorId(id);
            
            // Validar que la factura puede ser anulada
            if (!factura.puedeSerAnulada()) {
                redirectAttributes.addFlashAttribute("error", 
                    "No se puede anular la factura. Estado actual: " + factura.getEstado().getDescripcion() +
                    ". Solo se pueden anular facturas sin pagos o con saldo completo."
                );
                return "redirect:/facturas/" + id;
            }
            
            model.addAttribute("factura", factura);
            return "facturas/confirmar-anulacion";
            
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Factura no encontrada");
            return "redirect:/facturas";
        }
    }

    /**
     * Procesa la anulación de una factura.
     */
    @PostMapping("/{id}/anular")
    public String anularFactura(
            @PathVariable Long id, 
            @RequestParam String motivo,
            RedirectAttributes redirectAttributes) {
        try {
            Factura factura = facturaService.anularFactura(id, motivo);
            
            redirectAttributes.addFlashAttribute("success", 
                String.format("Factura %04d-%08d anulada exitosamente. Se generó la nota de crédito correspondiente.",
                    factura.getSerie(), factura.getNroFactura())
            );
            return "redirect:/facturas/" + id;
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/facturas/" + id;
        }
    }

    /**
     * Muestra el formulario para emitir una factura proporcional.
     */
    @GetMapping("/nueva-proporcional")
    public String mostrarFormularioFacturaProporcional(Model model) {
        model.addAttribute("clientes", clienteService.obtenerTodosLosClientes());
        model.addAttribute("fechaEmision", LocalDate.now());
        return "facturas/formulario-proporcional";
    }

    /**
     * Procesa el formulario de emisión de factura proporcional.
     * Calcula el monto proporcional basado en el rango de fechas especificado.
     */
    @PostMapping("/nueva-proporcional")
    public String emitirFacturaProporcional(
            @RequestParam Long clienteId,
            @RequestParam LocalDate inicioPeriodo,
            @RequestParam LocalDate finPeriodo,
            @RequestParam LocalDate fechaEmision,
            @RequestParam LocalDate fechaVencimiento,
            @RequestParam(required = false) Double porcentajeDescuento,
            @RequestParam(required = false) String motivoDescuento,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        try {
            Factura factura = facturaService.emitirFacturaProporcional(
                clienteId,
                inicioPeriodo,
                finPeriodo,
                fechaEmision,
                fechaVencimiento,
                porcentajeDescuento,
                motivoDescuento
            );
            
            redirectAttributes.addFlashAttribute("success", 
                String.format("Factura proporcional %04d-%08d emitida exitosamente. Total: $%.2f", 
                    factura.getSerie(), 
                    factura.getNroFactura(), 
                    factura.getTotal())
            );
            return "redirect:/facturas/" + factura.getIdFactura();
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("clientes", clienteService.obtenerTodosLosClientes());
            model.addAttribute("clienteId", clienteId);
            model.addAttribute("inicioPeriodo", inicioPeriodo);
            model.addAttribute("finPeriodo", finPeriodo);
            model.addAttribute("fechaEmision", fechaEmision);
            model.addAttribute("fechaVencimiento", fechaVencimiento);
            model.addAttribute("porcentajeDescuento", porcentajeDescuento);
            model.addAttribute("motivoDescuento", motivoDescuento);
            return "facturas/formulario-proporcional";
        }
    }
}
