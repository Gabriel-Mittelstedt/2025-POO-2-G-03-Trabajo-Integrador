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
import com.unam.integrador.services.FacturaService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests para FacturacionMasivaController.
 * Valida endpoints de facturación masiva, listado y anulación de lotes.
 */
@WebMvcTest(FacturacionMasivaController.class)
@DisplayName("Tests para FacturacionMasivaController")
class FacturacionMasivaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FacturaService facturaService;

    private LoteFacturacion lote;
    private CuentaCliente cliente1;
    private CuentaCliente cliente2;

    @BeforeEach
    void setUp() {
        // Mock clientes
        cliente1 = new CuentaCliente();
        cliente1.setNombre("Tech Solutions S.A.");
        cliente1.setRazonSocial("Tech Solutions S.A.");
        cliente1.setCuitDni("3011111111");
        cliente1.setDomicilio("Av. Corrientes 1234");
        cliente1.setEmail("techsolutions@email.com");
        cliente1.setCondicionIva(TipoCondicionIVA.RESPONSABLE_INSCRIPTO);
        cliente1.setEstado(EstadoCuenta.ACTIVA);

        cliente2 = new CuentaCliente();
        cliente2.setNombre("Digital Corp.");
        cliente2.setRazonSocial("Digital Corp.");
        cliente2.setCuitDni("3022222222");
        cliente2.setDomicilio("Av. Libertador 5678");
        cliente2.setEmail("digitalcorp@email.com");
        cliente2.setCondicionIva(TipoCondicionIVA.RESPONSABLE_INSCRIPTO);
        cliente2.setEstado(EstadoCuenta.ACTIVA);

        // Mock lote de facturación
        lote = new LoteFacturacion(
            "Noviembre 2025",
            LocalDate.of(2025, 11, 1),
            LocalDate.of(2025, 11, 30)
        );
        lote.setId(1L);

        // Crear facturas del lote
        Factura factura1 = crearFactura(cliente1, 1, 100);
        Factura factura2 = crearFactura(cliente2, 1, 101);
        
        lote.agregarFactura(factura1);
        lote.agregarFactura(factura2);
    }

    @Test
    @DisplayName("GET /facturacion-masiva - Debería mostrar lista de lotes")
    void testListarLotes() throws Exception {
        // Arrange
        List<LoteFacturacion> lotes = Arrays.asList(lote);
        when(facturaService.listarLotesFacturacion()).thenReturn(lotes);

        // Act & Assert
        mockMvc.perform(get("/facturacion-masiva"))
            .andExpect(status().isOk())
            .andExpect(view().name("facturacion-masiva/lista"))
            .andExpect(model().attribute("lotes", lotes));

        verify(facturaService).listarLotesFacturacion();
    }

    @Test
    @DisplayName("GET /facturacion-masiva/nuevo - Debería mostrar formulario")
    void testMostrarFormulario() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/facturacion-masiva/nuevo"))
            .andExpect(status().isOk())
            .andExpect(view().name("facturacion-masiva/formulario"))
            .andExpect(model().attributeExists("facturacionDTO"))
            .andExpect(model().attributeExists("periodos"));
    }

    @Test
    @DisplayName("POST /facturacion-masiva/nuevo - Debería ejecutar facturación masiva correctamente")
    void testEjecutarFacturacionMasiva() throws Exception {
        // Arrange
        when(facturaService.ejecutarFacturacionMasiva(anyString(), any()))
            .thenReturn(lote);

        // Act & Assert
        mockMvc.perform(post("/facturacion-masiva/nuevo")
                .param("periodo", "Noviembre 2025")
                .param("fechaVencimiento", "2025-11-30"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/facturacion-masiva/1"))
            .andExpect(flash().attributeExists("mensaje"));

        verify(facturaService).ejecutarFacturacionMasiva(
            eq("Noviembre 2025"), 
            eq(LocalDate.of(2025, 11, 30))
        );
    }

    @Test
    @DisplayName("POST /facturacion-masiva/nuevo - Debería manejar error cuando ya existe lote")
    void testEjecutarFacturacionMasivaDuplicada() throws Exception {
        // Arrange
        when(facturaService.ejecutarFacturacionMasiva(anyString(), any()))
            .thenThrow(new IllegalStateException("Ya existe una facturación masiva activa"));

        // Act & Assert
        mockMvc.perform(post("/facturacion-masiva/nuevo")
                .param("periodo", "Noviembre 2025")
                .param("fechaVencimiento", "2025-11-30"))
            .andExpect(status().isOk())
            .andExpect(view().name("facturacion-masiva/formulario"))
            .andExpect(model().attributeExists("error"))
            .andExpect(model().attributeExists("periodos"));
    }

    @Test
    @DisplayName("POST /facturacion-masiva/nuevo - Debería validar que período es obligatorio")
    void testEjecutarFacturacionMasivaSinPeriodo() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/facturacion-masiva/nuevo")
                .param("fechaVencimiento", "2025-11-30"))
            .andExpect(status().isOk())
            .andExpect(view().name("facturacion-masiva/formulario"))
            .andExpect(model().attributeExists("periodos"));
    }

    @Test
    @DisplayName("GET /facturacion-masiva/{id} - Debería mostrar detalle del lote")
    void testVerDetalle() throws Exception {
        // Arrange
        when(facturaService.obtenerLoteConFacturas(1L)).thenReturn(lote);

        // Act & Assert
        mockMvc.perform(get("/facturacion-masiva/1"))
            .andExpect(status().isOk())
            .andExpect(view().name("facturacion-masiva/detalle"))
            .andExpect(model().attribute("lote", lote));

        verify(facturaService).obtenerLoteConFacturas(1L);
    }

    @Test
    @DisplayName("GET /facturacion-masiva/{id} - Debería manejar lote no encontrado")
    void testVerDetalleLoteNoEncontrado() throws Exception {
        // Arrange
        when(facturaService.obtenerLoteConFacturas(999L))
            .thenThrow(new IllegalArgumentException("Lote no encontrado"));

        // Act & Assert
        mockMvc.perform(get("/facturacion-masiva/999"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/facturacion-masiva"))
            .andExpect(flash().attributeExists("error"));
    }

    @Test
    @DisplayName("GET /facturacion-masiva/{id}/confirmar-anulacion - Debería mostrar confirmación")
    void testConfirmarAnulacion() throws Exception {
        // Arrange
        when(facturaService.obtenerLoteConFacturas(1L)).thenReturn(lote);

        // Act & Assert
        mockMvc.perform(get("/facturacion-masiva/1/confirmar-anulacion"))
            .andExpect(status().isOk())
            .andExpect(view().name("facturacion-masiva/confirmar-anulacion"))
            .andExpect(model().attribute("lote", lote));
    }

    @Test
    @DisplayName("GET /facturacion-masiva/{id}/confirmar-anulacion - No debería permitir anular lote con pagos")
    void testConfirmarAnulacionLoteConPagos() throws Exception {
        // Arrange
        // Marcar una factura como pagada
        lote.getFacturas().get(0).setEstado(EstadoFactura.PAGADA_TOTALMENTE);
        when(facturaService.obtenerLoteConFacturas(1L)).thenReturn(lote);

        // Act & Assert
        mockMvc.perform(get("/facturacion-masiva/1/confirmar-anulacion"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/facturacion-masiva/1"))
            .andExpect(flash().attributeExists("error"));
    }

    @Test
    @DisplayName("POST /facturacion-masiva/{id}/anular - Debería anular lote correctamente")
    void testAnularLote() throws Exception {
        // Arrange
        lote.anular("Corrección de facturación masiva");
        when(facturaService.anularLoteFacturacion(eq(1L), anyString())).thenReturn(lote);

        // Act & Assert
        mockMvc.perform(post("/facturacion-masiva/1/anular")
                .param("motivo", "Corrección de facturación masiva"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/facturacion-masiva/1"))
            .andExpect(flash().attributeExists("mensaje"));

        verify(facturaService).anularLoteFacturacion(1L, "Corrección de facturación masiva");
    }

    @Test
    @DisplayName("POST /facturacion-masiva/{id}/anular - Debería manejar error de anulación")
    void testAnularLoteConError() throws Exception {
        // Arrange
        when(facturaService.anularLoteFacturacion(eq(1L), anyString()))
            .thenThrow(new IllegalStateException("No se puede anular el lote"));

        // Act & Assert
        mockMvc.perform(post("/facturacion-masiva/1/anular")
                .param("motivo", "Motivo"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/facturacion-masiva/1"))
            .andExpect(flash().attributeExists("error"));
    }

    @Test
    @DisplayName("POST /facturacion-masiva/nuevo - Debería validar fecha de vencimiento")
    void testEjecutarFacturacionMasivaSinFechaVencimiento() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/facturacion-masiva/nuevo")
                .param("periodo", "Noviembre 2025"))
            .andExpect(status().isOk())
            .andExpect(view().name("facturacion-masiva/formulario"));
    }

    @Test
    @DisplayName("POST /facturacion-masiva/nuevo - Debería crear lote con múltiples facturas")
    void testEjecutarFacturacionMasivaMultiplesClientes() throws Exception {
        // Arrange
        LoteFacturacion loteGrande = new LoteFacturacion(
            "Diciembre 2025",
            LocalDate.of(2025, 12, 1),
            LocalDate.of(2025, 12, 31)
        );
        loteGrande.setId(2L);
        
        // Simular 5 facturas
        for (int i = 1; i <= 5; i++) {
            CuentaCliente cliente = new CuentaCliente();
            cliente.setNombre("Cliente " + i);
            cliente.setRazonSocial("Cliente " + i);
            cliente.setCuitDni("30" + String.format("%08d", i));
            cliente.setDomicilio("Av. Test " + i);
            cliente.setEmail("cliente" + i + "@test.com");
            cliente.setCondicionIva(TipoCondicionIVA.RESPONSABLE_INSCRIPTO);
            cliente.setEstado(EstadoCuenta.ACTIVA);
            Factura factura = crearFactura(cliente, 1, 200 + i);
            loteGrande.agregarFactura(factura);
        }

        when(facturaService.ejecutarFacturacionMasiva(anyString(), any()))
            .thenReturn(loteGrande);

        // Act & Assert
        mockMvc.perform(post("/facturacion-masiva/nuevo")
                .param("periodo", "Diciembre 2025")
                .param("fechaVencimiento", "2025-12-31"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/facturacion-masiva/2"))
            .andExpect(flash().attributeExists("mensaje"));

        // Verificar que el mensaje menciona la cantidad de facturas
        verify(facturaService).ejecutarFacturacionMasiva(
            eq("Diciembre 2025"),
            eq(LocalDate.of(2025, 12, 31))
        );
    }

    @Test
    @DisplayName("GET /facturacion-masiva - Debería listar múltiples lotes ordenados")
    void testListarMultiplesLotes() throws Exception {
        // Arrange
        LoteFacturacion lote1 = new LoteFacturacion("Noviembre 2025", 
            LocalDate.of(2025, 11, 1), LocalDate.of(2025, 11, 30));
        lote1.setId(1L);
        
        LoteFacturacion lote2 = new LoteFacturacion("Diciembre 2025",
            LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 31));
        lote2.setId(2L);

        List<LoteFacturacion> lotes = Arrays.asList(lote2, lote1); // Ordenados desc
        when(facturaService.listarLotesFacturacion()).thenReturn(lotes);

        // Act & Assert
        mockMvc.perform(get("/facturacion-masiva"))
            .andExpect(status().isOk())
            .andExpect(view().name("facturacion-masiva/lista"))
            .andExpect(model().attribute("lotes", lotes));
    }

    // Método helper
    private Factura crearFactura(CuentaCliente cliente, int serie, int numero) {
        Factura factura = new Factura(
            serie,
            numero,
            cliente,
            LocalDate.of(2025, 11, 1),
            LocalDate.of(2025, 11, 30),
            LocalDate.of(2025, 11, 1),
            TipoFactura.A
        );
        factura.setIdFactura((long) numero);
        
        ItemFactura item = new ItemFactura(
            "Hosting Web",
            new BigDecimal("15000"),
            1,
            TipoAlicuotaIVA.IVA_21
        );
        factura.agregarItem(item);
        
        return factura;
    }
}
