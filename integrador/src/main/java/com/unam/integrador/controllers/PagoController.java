package com.unam.integrador.controllers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.unam.integrador.model.CuentaCliente;
import com.unam.integrador.model.Recibo;
import com.unam.integrador.model.enums.MetodoPago;

/**
 * Controlador mínimo para renderizar las pantallas de Pagos.
 *
 * Nota: Este controlador solo prepara los modelos necesarios para que
 * las vistas se rendericen. No contiene lógica de negocio ni llamadas
 * a servicios reales.
 */
@Controller
@RequestMapping("/pagos")
public class PagoController {

    @GetMapping
    public String listar(@RequestParam(required = false) String metodoPago,
                         @RequestParam(required = false) String fechaInicio,
                         @RequestParam(required = false) String fechaFin,
                         Model model) {
        model.addAttribute("pagos", new ArrayList<>());
        model.addAttribute("metodosPago", buildMetodosPago());
        return "pagos/lista";
    }

    @GetMapping("/{id}")
    public String verDetalle(@PathVariable Long id, Model model) {
        // Crear datos de ejemplo mínimos para que la plantilla se renderice
        PagoDTO pago = new PagoDTO();
        pago.setId(id);
        pago.setFechaPago(LocalDate.now());
        pago.setMontoTotal(BigDecimal.ZERO);
        CuentaCliente cliente = new CuentaCliente();
        cliente.setNombre("Cliente de ejemplo");
        pago.setCliente(cliente);
        pago.setMetodoPago(MetodoPago.EFECTIVO.name());
        pago.setReferencia("-");
        pago.setObservaciones(null);

        Recibo recibo = new Recibo();
        recibo.setNumero("R-EX-0001");

        model.addAttribute("pago", pago);
        model.addAttribute("recibo", recibo);
        return "pagos/detalle";
    }

    @GetMapping("/nuevo-combinado")
    public String mostrarFormularioCombinado(@RequestParam(required = false) Long clienteId, Model model) {
        // Datos de ejemplo: clientes (usar clase ligera ClienteView para poder setear id)
        List<ClienteView> clientes = new ArrayList<>();
        clientes.add(new ClienteView(1L, "ACME S.A."));
        clientes.add(new ClienteView(2L, "Distribuciones Pérez"));

        // Facturas de ejemplo según clienteId (simulación)
        List<FacturaView> facturas = new ArrayList<>();
        if (clienteId != null) {
            if (clienteId.equals(1L)) {
                facturas.add(new FacturaView(101L, "F-000101", "2025-09", new BigDecimal("1500.00"), new BigDecimal("1500.00"), "PENDIENTE", "Impaga"));
                facturas.add(new FacturaView(102L, "F-000102", "2025-10", new BigDecimal("750.00"), new BigDecimal("750.00"), "PENDIENTE", "Impaga"));
            } else if (clienteId.equals(2L)) {
                facturas.add(new FacturaView(201L, "F-000201", "2025-08", new BigDecimal("500.00"), new BigDecimal("200.00"), "PARCIAL", "Parcial"));
            }
        }

        model.addAttribute("clientes", clientes);
        model.addAttribute("facturas", facturas);
        model.addAttribute("metodosPago", buildMetodosPago());

        PagoDTO pagoDTO = new PagoDTO();
        pagoDTO.setClienteId(clienteId);
        model.addAttribute("pagoDTO", pagoDTO);
        model.addAttribute("selectedClienteId", clienteId);

        return "pagos/formulario-combinado";
    }

    @PostMapping("/nuevo-combinado")
    public String guardarCombinado(@ModelAttribute PagoDTO pagoDTO,
                                   @RequestParam(required = false) String[] facturaId,
                                   @RequestParam(required = false) BigDecimal montoTotal,
                                   @RequestParam Map<String,String> allParams,
                                   RedirectAttributes redirect) {
        // Procesar facturas seleccionadas y montos por factura (simulación)
        if (facturaId == null || facturaId.length == 0) {
            redirect.addFlashAttribute("error", "Debe seleccionar al menos una factura para pagar");
            return "redirect:/pagos/nuevo-combinado?clienteId=" + (pagoDTO.getClienteId() != null ? pagoDTO.getClienteId() : "");
        }

        List<String> asignaciones = new ArrayList<>();
        BigDecimal suma = BigDecimal.ZERO;
        // facturaId puede venir como array de strings; parsear cada elemento a Long
        for (String fidStr : facturaId) {
            if (fidStr == null) continue;
            // Manejar valores recibidos como "101" o como cadena con comas
            String[] parts = fidStr.split(",");
            for (String p : parts) {
                String trimmed = p.trim();
                if (trimmed.isEmpty()) continue;
                Long fidLong = null;
                try { fidLong = Long.valueOf(trimmed); } catch (NumberFormatException ex) { continue; }
                String key = "monto_" + fidLong;
                String val = allParams.get(key);
                BigDecimal monto = BigDecimal.ZERO;
                try {
                    if (val != null && !val.isBlank()) monto = new BigDecimal(val);
                } catch (NumberFormatException e) {
                    monto = BigDecimal.ZERO;
                }
                suma = suma.add(monto);
                asignaciones.add("Factura " + fidLong + ": $" + monto);
            }
        }

        // Si el montoTotal fue enviado en el formulario, usarlo; sino intentar desde pagoDTO
        BigDecimal recibido = montoTotal != null ? montoTotal : pagoDTO.getMontoTotal();
        if (recibido == null) recibido = suma;

        if (suma.compareTo(BigDecimal.ZERO) <= 0) {
            redirect.addFlashAttribute("error", "Los montos asignados deben ser mayores a 0");
            return "redirect:/pagos/nuevo-combinado?clienteId=" + (pagoDTO.getClienteId() != null ? pagoDTO.getClienteId() : "");
        }

        if (suma.compareTo(recibido) > 0) {
            redirect.addFlashAttribute("error", "La suma de montos asignados excede el Monto Total recibido");
            return "redirect:/pagos/nuevo-combinado?clienteId=" + (pagoDTO.getClienteId() != null ? pagoDTO.getClienteId() : "");
        }

        redirect.addFlashAttribute("mensaje", "(Simulación) Pago combinado registrado: " + String.join("; ", asignaciones));
        return "redirect:/pagos";
    }

    @GetMapping("/nuevo-parcial")
    public String mostrarFormularioParcial(@RequestParam(required = false) Long facturaId, Model model) {
        model.addAttribute("factura", new ArrayList<>());
        model.addAttribute("metodosPago", buildMetodosPago());
        return "pagos/formulario-parcial";
    }

    @PostMapping("/nuevo-parcial")
    public String guardarParcial(@RequestParam Long facturaId,
                                 @RequestParam BigDecimal montoParcial,
                                 RedirectAttributes redirect) {
        redirect.addFlashAttribute("mensaje", "(Simulación) Pago parcial registrado");
        return "redirect:/pagos";
    }

    @GetMapping("/nuevo-total")
    public String mostrarFormularioTotal(@RequestParam(required = false) Long facturaId, Model model) {
        model.addAttribute("factura", new ArrayList<>());
        model.addAttribute("metodosPago", buildMetodosPago());
        return "pagos/formulario-total";
    }

    @PostMapping("/nuevo-total")
    public String guardarTotal(@RequestParam Long facturaId, RedirectAttributes redirect) {
        redirect.addFlashAttribute("mensaje", "(Simulación) Pago total registrado");
        return "redirect:/pagos";
    }

    // --- Helpers y DTOs mínimos para las vistas ---

    private List<MetodoPagoView> buildMetodosPago() {
        List<MetodoPagoView> list = new ArrayList<>();
        for (MetodoPago m : MetodoPago.values()) {
            String desc = switch (m) {
                case EFECTIVO -> "Efectivo";
                case TRANSFERENCIA -> "Transferencia";
                case TARJETA -> "Tarjeta";
            };
            list.add(new MetodoPagoView(m.name(), desc));
        }
        return list;
    }

    public static class MetodoPagoView {
        private String codigo;
        private String descripcion;

        public MetodoPagoView() {}

        public MetodoPagoView(MetodoPago m, String descripcion) {
            this.codigo = m.name();
            this.descripcion = descripcion;
        }

        public MetodoPagoView(String codigo, String descripcion) {
            this.codigo = codigo;
            this.descripcion = descripcion;
        }

        public String getCodigo() { return codigo; }
        public void setCodigo(String codigo) { this.codigo = codigo; }
        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    }

    public static class PagoDTO {
        private Long id;
        private Long clienteId;
        private LocalDate fechaPago;
        private BigDecimal montoTotal;
        private CuentaCliente cliente;
        private String metodoPago;
        private String referencia;
        private String observaciones;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getClienteId() { return clienteId; }
        public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
        public LocalDate getFechaPago() { return fechaPago; }
        public void setFechaPago(LocalDate fechaPago) { this.fechaPago = fechaPago; }
        public BigDecimal getMontoTotal() { return montoTotal; }
        public void setMontoTotal(BigDecimal montoTotal) { this.montoTotal = montoTotal; }
        public CuentaCliente getCliente() { return cliente; }
        public void setCliente(CuentaCliente cliente) { this.cliente = cliente; }
        public String getMetodoPago() { return metodoPago; }
        public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }
        public String getReferencia() { return referencia; }
        public void setReferencia(String referencia) { this.referencia = referencia; }
        public String getObservaciones() { return observaciones; }
        public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    }

    // Clase ligera para representar clientes en la vista (permite setear id)
    public static class ClienteView {
        private Long id;
        private String nombre;

        public ClienteView() {}
        public ClienteView(Long id, String nombre) { this.id = id; this.nombre = nombre; }
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
    }

    // Clase auxiliar para simular facturas en la vista
    public static class FacturaView {
        private Long id;
        private String numeroFactura;
        private String periodo;
        private BigDecimal total;
        private BigDecimal saldoPendiente;
        private EstadoView estado;

        public FacturaView() {}

        public FacturaView(Long id, String numeroFactura, String periodo, BigDecimal total, BigDecimal saldoPendiente, String estadoCodigo, String estadoDescripcion) {
            this.id = id;
            this.numeroFactura = numeroFactura;
            this.periodo = periodo;
            this.total = total;
            this.saldoPendiente = saldoPendiente;
            this.estado = new EstadoView(estadoCodigo, estadoDescripcion);
        }

        public Long getId() { return id; }
        public String getNumeroFactura() { return numeroFactura; }
        public String getPeriodo() { return periodo; }
        public BigDecimal getTotal() { return total; }
        public BigDecimal getSaldoPendiente() { return saldoPendiente; }
        public EstadoView getEstado() { return estado; }
    }

    public static class EstadoView {
        private String codigo;
        private String descripcion;

        public EstadoView() {}
        public EstadoView(String codigo, String descripcion) { this.codigo = codigo; this.descripcion = descripcion; }

        // Thymeleaf template calls factura.estado.name(), so expose name()
        public String name() { return codigo; }

        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    }

    
}
