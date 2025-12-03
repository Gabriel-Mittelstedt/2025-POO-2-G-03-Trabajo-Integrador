package com.unam.integrador.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.unam.integrador.model.*;
import com.unam.integrador.model.enums.*;
import com.unam.integrador.services.CuentaClienteService;
import com.unam.integrador.services.FacturaService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests para FacturaViewController.
 * Valida endpoints HTTP y respuestas de vistas del controlador.
 */
@WebMvcTest(FacturaViewController.class)
@DisplayName("Tests para FacturaViewController")
class FacturaViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FacturaService facturaService;

    @MockBean
    private CuentaClienteService clienteService;

    private Factura factura;
    private CuentaCliente cliente;

    @BeforeEach
    void setUp() {
        // Mock cliente
        cliente = new CuentaCliente();
        cliente.setNombre("Tech Solutions S.A.");
        cliente.setRazonSocial("Tech Solutions S.A.");
        cliente.setCuitDni("3012345678");
        cliente.setDomicilio("Av. Corrientes 1234");
        cliente.setEmail("techsolutions@email.com");
        cliente.setCondicionIva(TipoCondicionIVA.RESPONSABLE_INSCRIPTO);
        cliente.setEstado(EstadoCuenta.ACTIVA);

        // Mock factura
        factura = new Factura(
            1, 100, cliente,
            LocalDate.of(2025, 11, 1),
            LocalDate.of(2025, 11, 30),
            LocalDate.of(2025, 11, 1),
            TipoFactura.A
        );
        factura.setIdFactura(1L);
        
        ItemFactura item = new ItemFactura("Hosting Web", new BigDecimal("15000"), 1, TipoAlicuotaIVA.IVA_21);
        factura.agregarItem(item);
    }

    @Test
    @DisplayName("GET /facturas - Debería mostrar lista de facturas")
    void testListarFacturas() throws Exception {
        // Arrange
        List<Factura> facturas = Arrays.asList(factura);
        when(facturaService.listarFacturasFiltradas(null, null, null)).thenReturn(facturas);
        when(facturaService.actualizarFacturasVencidas()).thenReturn(0);

        // Act & Assert
        mockMvc.perform(get("/facturas"))
            .andExpect(status().isOk())
            .andExpect(view().name("facturas/lista"))
            .andExpect(model().attributeExists("facturas"))
            .andExpect(model().attributeExists("estados"))
            .andExpect(model().attributeExists("tipos"));

        verify(facturaService).actualizarFacturasVencidas();
        verify(facturaService).listarFacturasFiltradas(null, null, null);
    }

    @Test
    @DisplayName("GET /facturas - Debería aplicar filtros correctamente")
    void testListarFacturasConFiltros() throws Exception {
        // Arrange
        when(facturaService.listarFacturasFiltradas("PENDIENTE", "A", "Noviembre 2025"))
            .thenReturn(Arrays.asList(factura));

        // Act & Assert
        mockMvc.perform(get("/facturas")
                .param("estado", "PENDIENTE")
                .param("tipoFactura", "A")
                .param("mes", "11")
                .param("anio", "2025"))
            .andExpect(status().isOk())
            .andExpect(view().name("facturas/lista"))
            .andExpect(model().attribute("estado", "PENDIENTE"))
            .andExpect(model().attribute("tipoFactura", "A"))
            .andExpect(model().attribute("mesSeleccionado", 11))
            .andExpect(model().attribute("anioSeleccionado", 2025));
    }

    @Test
    @DisplayName("GET /facturas/nueva-individual - Debería mostrar formulario")
    void testMostrarFormularioFacturaIndividual() throws Exception {
        // Arrange
        List<CuentaCliente> clientes = Arrays.asList(cliente);
        when(clienteService.obtenerTodosLosClientes()).thenReturn(clientes);

        // Act & Assert
        mockMvc.perform(get("/facturas/nueva-individual"))
            .andExpect(status().isOk())
            .andExpect(view().name("facturas/formulario-individual"))
            .andExpect(model().attributeExists("clientes"))
            .andExpect(model().attributeExists("periodos"))
            .andExpect(model().attributeExists("fechaEmision"));

        verify(clienteService).obtenerTodosLosClientes();
    }

    @Test
    @DisplayName("POST /facturas/nueva-individual - Debería emitir factura correctamente")
    void testEmitirFacturaIndividual() throws Exception {
        // Arrange
        when(facturaService.emitirFacturaDesdeServiciosContratados(
            anyLong(), any(), any(), any(), any(), any()))
            .thenReturn(factura);

        // Act & Assert
        mockMvc.perform(post("/facturas/nueva-individual")
                .param("clienteId", "1")
                .param("periodo", "Noviembre 2025")
                .param("fechaEmision", "2025-11-01")
                .param("fechaVencimiento", "2025-11-30"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/facturas/1"))
            .andExpect(flash().attributeExists("success"));

        verify(facturaService).emitirFacturaDesdeServiciosContratados(
            eq(1L), any(), any(), any(), isNull(), isNull());
    }

    @Test
    @DisplayName("POST /facturas/nueva-individual - Debería manejar errores correctamente")
    void testEmitirFacturaIndividualConError() throws Exception {
        // Arrange
        when(facturaService.emitirFacturaDesdeServiciosContratados(
            anyLong(), any(), any(), any(), any(), any()))
            .thenThrow(new IllegalStateException("Ya existe una factura para este período"));
        when(clienteService.obtenerTodosLosClientes()).thenReturn(Arrays.asList(cliente));

        // Act & Assert
        mockMvc.perform(post("/facturas/nueva-individual")
                .param("clienteId", "1")
                .param("periodo", "Noviembre 2025")
                .param("fechaEmision", "2025-11-01")
                .param("fechaVencimiento", "2025-11-30"))
            .andExpect(status().isOk())
            .andExpect(view().name("facturas/formulario-individual"))
            .andExpect(model().attributeExists("error"));
    }

    @Test
    @DisplayName("GET /facturas/{id} - Debería mostrar detalle de factura")
    void testVerDetalle() throws Exception {
        // Arrange
        when(facturaService.obtenerFacturaPorId(1L)).thenReturn(factura);
        when(facturaService.actualizarFacturasVencidas()).thenReturn(0);

        // Act & Assert
        mockMvc.perform(get("/facturas/1"))
            .andExpect(status().isOk())
            .andExpect(view().name("facturas/detalle"))
            .andExpect(model().attribute("factura", factura));

        verify(facturaService).obtenerFacturaPorId(1L);
    }

    @Test
    @DisplayName("GET /facturas/nueva-proporcional - Debería mostrar formulario proporcional")
    void testMostrarFormularioFacturaProporcional() throws Exception {
        // Arrange
        when(clienteService.obtenerTodosLosClientes()).thenReturn(Arrays.asList(cliente));

        // Act & Assert
        mockMvc.perform(get("/facturas/nueva-proporcional"))
            .andExpect(status().isOk())
            .andExpect(view().name("facturas/formulario-proporcional"))
            .andExpect(model().attributeExists("clientes"));
    }

    @Test
    @DisplayName("POST /facturas/nueva-proporcional - Debería emitir factura proporcional")
    void testEmitirFacturaProporcional() throws Exception {
        // Arrange
        when(facturaService.emitirFacturaProporcional(
            anyLong(), any(), any(), any(), any(), any(), any()))
            .thenReturn(factura);

        // Act & Assert
        mockMvc.perform(post("/facturas/nueva-proporcional")
                .param("clienteId", "1")
                .param("inicioPeriodo", "2025-11-15")
                .param("finPeriodo", "2025-11-30")
                .param("fechaEmision", "2025-11-15")
                .param("fechaVencimiento", "2025-11-30"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/facturas/1"))
            .andExpect(flash().attributeExists("success"));
    }

    @Test
    @DisplayName("GET /facturas/{id}/confirmar-anulacion - Debería mostrar confirmación")
    void testConfirmarAnulacion() throws Exception {
        // Arrange
        when(facturaService.obtenerFacturaPorId(1L)).thenReturn(factura);

        // Act & Assert
        mockMvc.perform(get("/facturas/1/confirmar-anulacion"))
            .andExpect(status().isOk())
            .andExpect(view().name("facturas/confirmar-anulacion"))
            .andExpect(model().attribute("factura", factura));
    }

    @Test
    @DisplayName("GET /facturas/{id}/confirmar-anulacion - No debería permitir anular factura pagada")
    void testConfirmarAnulacionFacturaPagada() throws Exception {
        // Arrange
        factura.setEstado(EstadoFactura.PAGADA_TOTALMENTE);
        when(facturaService.obtenerFacturaPorId(1L)).thenReturn(factura);

        // Act & Assert
        mockMvc.perform(get("/facturas/1/confirmar-anulacion"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/facturas/1"))
            .andExpect(flash().attributeExists("error"));
    }

    @Test
    @DisplayName("POST /facturas/{id}/anular - Debería anular factura correctamente")
    void testAnularFactura() throws Exception {
        // Arrange
        when(facturaService.anularFactura(eq(1L), anyString())).thenReturn(factura);

        // Act & Assert
        mockMvc.perform(post("/facturas/1/anular")
                .param("motivo", "Corrección de datos"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/facturas/1"))
            .andExpect(flash().attributeExists("success"));

        verify(facturaService).anularFactura(1L, "Corrección de datos");
    }

    @Test
    @DisplayName("POST /facturas/{id}/anular - Debería manejar error de anulación")
    void testAnularFacturaConError() throws Exception {
        // Arrange
        when(facturaService.anularFactura(eq(1L), anyString()))
            .thenThrow(new IllegalStateException("No se puede anular la factura"));

        // Act & Assert
        mockMvc.perform(post("/facturas/1/anular")
                .param("motivo", "Motivo"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/facturas/1"))
            .andExpect(flash().attributeExists("error"));
    }

    @Test
    @DisplayName("GET /facturas/cliente/{clienteId} - Debería listar facturas del cliente")
    void testListarFacturasPorCliente() throws Exception {
        // Arrange
        when(facturaService.listarFacturasPorCliente(1L)).thenReturn(Arrays.asList(factura));
        when(clienteService.obtenerClientePorId(1L)).thenReturn(cliente);

        // Act & Assert
        mockMvc.perform(get("/facturas/cliente/1"))
            .andExpect(status().isOk())
            .andExpect(view().name("facturas/lista"))
            .andExpect(model().attributeExists("facturas"))
            .andExpect(model().attributeExists("cliente"));
    }
}
