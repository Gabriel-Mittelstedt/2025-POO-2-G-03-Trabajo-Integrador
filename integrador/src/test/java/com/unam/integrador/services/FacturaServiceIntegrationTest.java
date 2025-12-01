package com.unam.integrador.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.unam.integrador.model.*;
import com.unam.integrador.model.enums.*;
import com.unam.integrador.repositories.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración para FacturaService.
 * Usa base de datos H2 en memoria para probar operaciones reales con persistencia.
 * Enfocado en emisión, anulación y facturación masiva.
 */
@SpringBootTest
@Transactional
@DisplayName("Tests de integración para FacturaService")
class FacturaServiceIntegrationTest {

    @Autowired
    private FacturaService facturaService;

    @Autowired
    private CuentaClienteRepositorie clienteRepository;

    @Autowired
    private ServicioRepository servicioRepository;


    private CuentaCliente cliente1;
    private CuentaCliente cliente2;

    @BeforeEach
    void setUp() {
        // Crear clientes con servicios mock
        cliente1 = crearClienteConServicios("Tech Solutions S.A.", "30123456789", 
            TipoCondicionIVA.RESPONSABLE_INSCRIPTO, 2);
        cliente2 = crearClienteConServicios("Digital Corp.", "30876543210", 
            TipoCondicionIVA.RESPONSABLE_INSCRIPTO, 1);
    }

    @Test
    @DisplayName("Debería emitir factura individual desde servicios contratados")
    void testEmitirFacturaIndividual() {
        // Arrange
        LocalDate periodo = LocalDate.of(2025, 11, 1);
        LocalDate fechaEmision = LocalDate.of(2025, 11, 1);
        LocalDate fechaVencimiento = LocalDate.of(2025, 11, 30);

        // Act
        Factura factura = facturaService.emitirFacturaDesdeServiciosContratados(
            cliente1.getId(),
            periodo,
            fechaEmision,
            fechaVencimiento,
            null,
            null
        );

        // Assert
        assertNotNull(factura);
        assertNotNull(factura.getIdFactura());
        assertEquals(2, factura.getDetalleFactura().size()); // 2 servicios
        assertEquals(EstadoFactura.PENDIENTE, factura.getEstado());
        
        // Total: (15000 + 8000) * 1.21 = 27830
        assertEquals(0, new BigDecimal("23000.00").compareTo(factura.getSubtotal()));
        assertEquals(0, new BigDecimal("27830.00").compareTo(factura.getTotal()));
    }

    @Test
    @DisplayName("Debería emitir factura con descuento del 15%")
    void testEmitirFacturaConDescuento() {
        // Arrange
        LocalDate periodo = LocalDate.of(2025, 11, 1);
        LocalDate fechaEmision = LocalDate.of(2025, 11, 1);
        LocalDate fechaVencimiento = LocalDate.of(2025, 11, 30);

        // Act
        Factura factura = facturaService.emitirFacturaDesdeServiciosContratados(
            cliente1.getId(),
            periodo,
            fechaEmision,
            fechaVencimiento,
            15.0,
            "Promoción cliente frecuente"
        );

        // Assert
        assertEquals(15.0, factura.getDescuento());
        assertEquals("Promoción cliente frecuente", factura.getMotivoDescuento());
        
        // Subtotal: 23000
        // Descuento: 3450 (15%)
        // Base: 19550
        // IVA: 4830
        // Total: 24380
        BigDecimal totalEsperado = new BigDecimal("24380.00");
        assertEquals(0, totalEsperado.compareTo(factura.getTotal()));
    }

    @Test
    @DisplayName("Debería lanzar excepción al emitir factura duplicada para mismo período")
    void testNoPermitirFacturaDuplicadaMismoPeriodo() {
        // Arrange
        LocalDate periodo = LocalDate.of(2025, 11, 1);
        LocalDate fechaEmision = LocalDate.of(2025, 11, 1);
        LocalDate fechaVencimiento = LocalDate.of(2025, 11, 30);

        // Emitir primera factura
        facturaService.emitirFacturaDesdeServiciosContratados(
            cliente1.getId(),
            periodo,
            fechaEmision,
            fechaVencimiento,
            null,
            null
        );

        // Act & Assert - Intentar emitir segunda factura
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> facturaService.emitirFacturaDesdeServiciosContratados(
                cliente1.getId(),
                periodo,
                fechaEmision,
                fechaVencimiento,
                null,
                null
            )
        );
        assertTrue(exception.getMessage().contains("Ya existe una factura emitida"));
    }

    @Test
    @DisplayName("Debería emitir factura proporcional correctamente")
    void testEmitirFacturaProporcional() {
        // Arrange - Cliente dado de alta el 15 de noviembre
        LocalDate inicioPeriodo = LocalDate.of(2025, 11, 15);
        LocalDate finPeriodo = LocalDate.of(2025, 11, 30);
        LocalDate fechaEmision = LocalDate.of(2025, 11, 15);
        LocalDate fechaVencimiento = LocalDate.of(2025, 11, 30);

        // Act
        Factura factura = facturaService.emitirFacturaProporcional(
            cliente1.getId(),
            inicioPeriodo,
            finPeriodo,
            fechaEmision,
            fechaVencimiento,
            null,
            null
        );

        // Assert
        assertNotNull(factura);
        assertEquals(2, factura.getDetalleFactura().size());
        
        // Verificar que los items tienen descripción proporcional
        assertTrue(factura.getDetalleFactura().get(0).getDescripcion().contains("15 al 30"));
        
        // Período: 16 días de 30 (proporción = 0.5333 con 4 decimales)
        // Hosting: 15000 * 0.5333 = 7999.50
        // Email: 8000 * 0.5333 = 4266.40
        // Subtotal: 12265.90
        BigDecimal subtotalEsperado = new BigDecimal("12265.90");
        assertEquals(0, subtotalEsperado.compareTo(factura.getSubtotal()));
    }

    @Test
    @DisplayName("Debería anular factura individual correctamente")
    void testAnularFacturaIndividual() {
        // Arrange
        Factura factura = facturaService.emitirFacturaDesdeServiciosContratados(
            cliente1.getId(),
            LocalDate.of(2025, 11, 1),
            LocalDate.of(2025, 11, 1),
            LocalDate.of(2025, 11, 30),
            null,
            null
        );

        // Act
        Factura facturaAnulada = facturaService.anularFactura(
            factura.getIdFactura(),
            "Error en facturación - corrección de datos"
        );

        // Assert
        assertEquals(EstadoFactura.ANULADA, facturaAnulada.getEstado());
        assertEquals(1, facturaAnulada.getNotasCredito().size());
        assertEquals(facturaAnulada.getTotal(), facturaAnulada.getNotasCredito().get(0).getMonto());
    }

    @Test
    @DisplayName("Debería ejecutar facturación masiva para múltiples clientes")
    void testEjecutarFacturacionMasiva() {
        // Arrange
        String periodo = "Diciembre 2025";
        LocalDate fechaVencimiento = LocalDate.of(2025, 12, 31);

        // Act
        LoteFacturacion lote = facturaService.ejecutarFacturacionMasiva(periodo, fechaVencimiento);

        // Assert
        assertNotNull(lote);
        assertNotNull(lote.getId());
        assertEquals(periodo, lote.getPeriodo());
        assertEquals(2, lote.getCantidadFacturas()); // 2 clientes activos
        assertFalse(lote.isAnulado());
        
        // Verificar que cada cliente tiene su factura
        List<Factura> facturas = lote.getFacturas();
        assertEquals(2, facturas.size());
        
        // Verificar montos
        assertTrue(lote.getMontoTotal().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("No debería permitir facturación masiva duplicada para mismo período")
    void testNoPermitirFacturacionMasivaDuplicada() {
        // Arrange
        String periodo = "Diciembre 2025";
        LocalDate fechaVencimiento = LocalDate.of(2025, 12, 31);
        
        // Ejecutar primera facturación
        facturaService.ejecutarFacturacionMasiva(periodo, fechaVencimiento);

        // Act & Assert - Intentar duplicar
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> facturaService.ejecutarFacturacionMasiva(periodo, fechaVencimiento)
        );
        assertTrue(exception.getMessage().contains("Ya existe una facturación masiva activa"));
    }

    @Test
    @DisplayName("Debería anular lote de facturación masiva correctamente")
    void testAnularLoteFacturacionMasiva() {
        // Arrange
        LoteFacturacion lote = facturaService.ejecutarFacturacionMasiva(
            "Diciembre 2025",
            LocalDate.of(2025, 12, 31)
        );

        // Act
        LoteFacturacion loteAnulado = facturaService.anularLoteFacturacion(
            lote.getId(),
            "Corrección de período - error administrativo"
        );

        // Assert
        assertTrue(loteAnulado.isAnulado());
        assertEquals("Corrección de período - error administrativo", loteAnulado.getMotivoAnulacion());
        
        // Verificar que todas las facturas fueron anuladas
        for (Factura factura : loteAnulado.getFacturas()) {
            assertEquals(EstadoFactura.ANULADA, factura.getEstado());
            assertFalse(factura.getNotasCredito().isEmpty());
        }
    }

    @Test
    @DisplayName("Debería listar facturas por cliente correctamente")
    void testListarFacturasPorCliente() {
        // Arrange
        facturaService.emitirFacturaDesdeServiciosContratados(
            cliente1.getId(),
            LocalDate.of(2025, 11, 1),
            LocalDate.of(2025, 11, 1),
            LocalDate.of(2025, 11, 30),
            null,
            null
        );

        // Act
        Iterable<Factura> facturas = facturaService.listarFacturasPorCliente(cliente1.getId());

        // Assert
        assertNotNull(facturas);
        assertEquals(1, ((List<Factura>) facturas).size());
    }

    @Test
    @DisplayName("Debería actualizar facturas vencidas correctamente")
    void testActualizarFacturasVencidas() {
        // Arrange - Crear factura con vencimiento pasado
        Factura facturaVencida = facturaService.emitirFacturaDesdeServiciosContratados(
            cliente1.getId(),
            LocalDate.of(2025, 10, 1),
            LocalDate.of(2025, 10, 1),
            LocalDate.of(2025, 10, 31), // Fecha pasada
            null,
            null
        );

        // Act
        int actualizadas = facturaService.actualizarFacturasVencidas();

        // Assert
        assertTrue(actualizadas > 0);
        
        // Recargar factura y verificar estado
        Factura facturaActualizada = facturaService.obtenerFacturaPorId(facturaVencida.getIdFactura());
        assertEquals(EstadoFactura.VENCIDA, facturaActualizada.getEstado());
    }

    @Test
    @DisplayName("Debería obtener lote con facturas cargadas")
    void testObtenerLoteConFacturas() {
        // Arrange
        LoteFacturacion lote = facturaService.ejecutarFacturacionMasiva(
            "Enero 2026",
            LocalDate.of(2026, 1, 31)
        );

        // Act
        LoteFacturacion loteRecuperado = facturaService.obtenerLoteConFacturas(lote.getId());

        // Assert
        assertNotNull(loteRecuperado);
        assertNotNull(loteRecuperado.getFacturas());
        assertEquals(2, loteRecuperado.getFacturas().size());
    }

    @Test
    @DisplayName("Debería listar lotes ordenados por fecha")
    void testListarLotesFacturacion() {
        // Arrange
        facturaService.ejecutarFacturacionMasiva("Diciembre 2025", LocalDate.of(2025, 12, 31));
        facturaService.ejecutarFacturacionMasiva("Enero 2026", LocalDate.of(2026, 1, 31));

        // Act
        List<LoteFacturacion> lotes = facturaService.listarLotesFacturacion();

        // Assert
        assertNotNull(lotes);
        assertEquals(2, lotes.size());
        // Deben estar ordenados de más reciente a más antiguo
        assertTrue(lotes.get(0).getFechaEjecucion().isAfter(lotes.get(1).getFechaEjecucion()) ||
                   lotes.get(0).getFechaEjecucion().isEqual(lotes.get(1).getFechaEjecucion()));
    }

    // Métodos helper para crear datos de prueba

    private CuentaCliente crearClienteConServicios(String nombre, String cuit, 
                                                     TipoCondicionIVA condicionIva, int cantidadServicios) {
        CuentaCliente cliente = new CuentaCliente();
        cliente.setNombre(nombre);
        cliente.setRazonSocial(nombre);
        cliente.setCuitDni(cuit);
        cliente.setDomicilio("Av. Corrientes 1234, CABA");
        cliente.setTelefono("11-1234-5678");
        cliente.setEmail(nombre.toLowerCase().replace(" ", "").replace(".", "") + "@email.com");
        cliente.setCondicionIva(condicionIva);
        cliente.setEstado(EstadoCuenta.ACTIVA);
        
        // Crear servicios contratados mock
        List<ServicioContratado> serviciosContratados = new ArrayList<>();
        for (int i = 0; i < cantidadServicios; i++) {
            // Buscar o crear el servicio
            final String nombreServicio = i == 0 ? "Hosting Web" : "Email";
            final int index = i;
            Servicio servicio = servicioRepository.findByNombre(nombreServicio).orElseGet(() -> {
                Servicio nuevo = new Servicio();
                nuevo.setNombre(nombreServicio);
                nuevo.setDescripcion("Servicio de " + nombreServicio);
                nuevo.setAlicuotaIVA(TipoAlicuotaIVA.IVA_21);
                nuevo.setPrecio(new BigDecimal(index == 0 ? "15000.00" : "8000.00"));
                nuevo.setActivo(true);
                return servicioRepository.save(nuevo);
            });
            
            ServicioContratado sc = new ServicioContratado();
            sc.setActivo(true);
            sc.setFechaAlta(LocalDate.now());
            sc.setPrecioContratado(servicio.getPrecio());
            sc.setServicio(servicio);
            sc.setCliente(cliente);
            serviciosContratados.add(sc);
        }
        
        cliente.setServiciosContratados(serviciosContratados);
        return clienteRepository.save(cliente);
    }
}
