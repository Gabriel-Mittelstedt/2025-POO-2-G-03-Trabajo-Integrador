package com.unam.integrador.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import com.unam.integrador.model.enums.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para LoteFacturacion.
 * Enfocado en facturación masiva, agregado de facturas y anulación de lotes.
 */
@DisplayName("Tests unitarios para LoteFacturacion")
class LoteFacturacionTest {

    private LoteFacturacion lote;
    private CuentaCliente cliente1;
    private CuentaCliente cliente2;

    @BeforeEach
    void setUp() {
        // Crear lote de facturación masiva para Noviembre 2025
        lote = new LoteFacturacion(
            "Noviembre 2025",
            LocalDate.of(2025, 11, 1),
            LocalDate.of(2025, 11, 30)
        );

        // Crear clientes mock
        cliente1 = mock(CuentaCliente.class);
        when(cliente1.getId()).thenReturn(1L);
        when(cliente1.getNombre()).thenReturn("Tech Solutions S.A.");
        when(cliente1.getCondicionIva()).thenReturn(TipoCondicionIVA.RESPONSABLE_INSCRIPTO);
        when(cliente1.getEstado()).thenReturn(EstadoCuenta.ACTIVA);

        cliente2 = mock(CuentaCliente.class);
        when(cliente2.getId()).thenReturn(2L);
        when(cliente2.getNombre()).thenReturn("Digital Marketing Corp.");
        when(cliente2.getCondicionIva()).thenReturn(TipoCondicionIVA.RESPONSABLE_INSCRIPTO);
        when(cliente2.getEstado()).thenReturn(EstadoCuenta.ACTIVA);
    }

    @Test
    @DisplayName("Debería crear lote con valores iniciales correctos")
    void testCrearLote() {
        // Assert
        assertEquals("Noviembre 2025", lote.getPeriodo());
        assertEquals(LocalDate.of(2025, 11, 1), lote.getPeriodoFecha());
        assertEquals(LocalDate.of(2025, 11, 30), lote.getFechaVencimiento());
        assertEquals(0, lote.getCantidadFacturas());
        assertEquals(BigDecimal.ZERO, lote.getMontoTotal());
        assertFalse(lote.isAnulado());
        assertNotNull(lote.getFechaEjecucion());
        assertTrue(lote.getFacturas().isEmpty());
    }

    @Test
    @DisplayName("Debería agregar factura y actualizar totales")
    void testAgregarFactura() {
        // Arrange
        Factura factura = crearFacturaConTotal(cliente1, new BigDecimal("15000.00"));

        // Act
        lote.agregarFactura(factura);

        // Assert
        assertEquals(1, lote.getCantidadFacturas());
        // Total real: 15000/1.21 = 12396.69 + IVA = 14999.9949
        assertEquals(0, lote.getMontoTotal().compareTo(new BigDecimal("14999.9949")));
        assertEquals(lote, factura.getLoteFacturacion());
        assertTrue(lote.getFacturas().contains(factura));
    }

    @Test
    @DisplayName("Debería agregar múltiples facturas y calcular total correcto")
    void testAgregarMultiplesFacturas() {
        // Arrange
        Factura factura1 = crearFacturaConTotal(cliente1, new BigDecimal("15000.00"));
        Factura factura2 = crearFacturaConTotal(cliente2, new BigDecimal("20000.00"));

        // Act
        lote.agregarFactura(factura1);
        lote.agregarFactura(factura2);

        // Assert
        assertEquals(2, lote.getCantidadFacturas());
        // 14999.9949 + 20000 con decimales
        assertTrue(lote.getMontoTotal().compareTo(new BigDecimal("34999.99")) > 0);
        assertTrue(lote.getMontoTotal().compareTo(new BigDecimal("35000.01")) < 0);
    }

    @Test
    @DisplayName("Debería lanzar excepción al agregar factura nula")
    void testAgregarFacturaNula() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> lote.agregarFactura(null)
        );
        assertEquals("La factura no puede ser nula", exception.getMessage());
    }

    @Test
    @DisplayName("Debería poder anular lote sin pagos")
    void testPuedeSerAnuladoSinPagos() {
        // Arrange
        Factura factura1 = crearFacturaConTotal(cliente1, new BigDecimal("10000.00"));
        Factura factura2 = crearFacturaConTotal(cliente2, new BigDecimal("15000.00"));
        lote.agregarFactura(factura1);
        lote.agregarFactura(factura2);

        // Assert
        assertTrue(lote.puedeSerAnulado());
    }

    @Test
    @DisplayName("No debería poder anular lote con facturas pagadas parcialmente")
    void testNoPuedeSerAnuladoConPagosParciales() {
        // Arrange
        Factura factura1 = crearFacturaConTotal(cliente1, new BigDecimal("10000.00"));
        Factura factura2 = crearFacturaConTotal(cliente2, new BigDecimal("15000.00"));
        
        // Marcar una factura como pagada parcialmente
        factura2.setEstado(EstadoFactura.PAGADA_PARCIALMENTE);
        
        lote.agregarFactura(factura1);
        lote.agregarFactura(factura2);

        // Assert
        assertFalse(lote.puedeSerAnulado());
    }

    @Test
    @DisplayName("No debería poder anular lote con facturas pagadas totalmente")
    void testNoPuedeSerAnuladoConPagosTotales() {
        // Arrange
        Factura factura1 = crearFacturaConTotal(cliente1, new BigDecimal("10000.00"));
        factura1.setEstado(EstadoFactura.PAGADA_TOTALMENTE);
        lote.agregarFactura(factura1);

        // Assert
        assertFalse(lote.puedeSerAnulado());
    }

    @Test
    @DisplayName("No debería poder anular lote ya anulado")
    void testNoPuedeSerAnuladoSiYaEstaAnulado() {
        // Arrange
        Factura factura = crearFacturaConTotal(cliente1, new BigDecimal("10000.00"));
        lote.agregarFactura(factura);
        lote.anular("Prueba de anulación");

        // Assert
        assertFalse(lote.puedeSerAnulado());
    }

    @Test
    @DisplayName("Debería anular lote correctamente")
    void testAnularLote() {
        // Arrange
        Factura factura1 = crearFacturaConTotal(cliente1, new BigDecimal("10000.00"));
        Factura factura2 = crearFacturaConTotal(cliente2, new BigDecimal("15000.00"));
        lote.agregarFactura(factura1);
        lote.agregarFactura(factura2);

        // Act
        lote.anular("Corrección de facturación masiva");

        // Assert
        assertTrue(lote.isAnulado());
        assertNotNull(lote.getFechaAnulacion());
        assertEquals("Corrección de facturación masiva", lote.getMotivoAnulacion());
    }

    @Test
    @DisplayName("Debería lanzar excepción al anular sin motivo")
    void testAnularSinMotivo() {
        // Arrange
        Factura factura = crearFacturaConTotal(cliente1, new BigDecimal("10000.00"));
        lote.agregarFactura(factura);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> lote.anular(null)
        );
        assertEquals("El motivo de anulación es obligatorio", exception.getMessage());
    }

    @Test
    @DisplayName("Debería lanzar excepción al anular lote con pagos")
    void testAnularLoteConPagos() {
        // Arrange
        Factura factura = crearFacturaConTotal(cliente1, new BigDecimal("10000.00"));
        factura.setEstado(EstadoFactura.PAGADA_TOTALMENTE);
        lote.agregarFactura(factura);

        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> lote.anular("Motivo")
        );
        assertTrue(exception.getMessage().contains("No se puede anular el lote"));
    }

    @Test
    @DisplayName("Debería obtener facturas activas (no anuladas)")
    void testGetFacturasActivas() {
        // Arrange
        Factura factura1 = crearFacturaConTotal(cliente1, new BigDecimal("10000.00"));
        Factura factura2 = crearFacturaConTotal(cliente2, new BigDecimal("15000.00"));
        Factura factura3 = crearFacturaConTotal(cliente1, new BigDecimal("8000.00"));
        
        factura2.setEstado(EstadoFactura.ANULADA); // Una anulada
        
        lote.agregarFactura(factura1);
        lote.agregarFactura(factura2);
        lote.agregarFactura(factura3);

        // Act
        var facturasActivas = lote.getFacturasActivas();

        // Assert
        assertEquals(2, facturasActivas.size());
        assertTrue(facturasActivas.contains(factura1));
        assertTrue(facturasActivas.contains(factura3));
        assertFalse(facturasActivas.contains(factura2));
    }

    @Test
    @DisplayName("Debería contar facturas anuladas correctamente")
    void testGetCantidadFacturasAnuladas() {
        // Arrange
        Factura factura1 = crearFacturaConTotal(cliente1, new BigDecimal("10000.00"));
        Factura factura2 = crearFacturaConTotal(cliente2, new BigDecimal("15000.00"));
        Factura factura3 = crearFacturaConTotal(cliente1, new BigDecimal("8000.00"));
        
        factura1.setEstado(EstadoFactura.ANULADA);
        factura2.setEstado(EstadoFactura.ANULADA);
        
        lote.agregarFactura(factura1);
        lote.agregarFactura(factura2);
        lote.agregarFactura(factura3);

        // Act
        int anuladas = lote.getCantidadFacturasAnuladas();

        // Assert
        assertEquals(2, anuladas);
    }

    @Test
    @DisplayName("Debería calcular monto total activo excluyendo anuladas")
    void testGetMontoTotalActivo() {
        // Arrange
        Factura factura1 = crearFacturaConTotal(cliente1, new BigDecimal("10000.00"));
        Factura factura2 = crearFacturaConTotal(cliente2, new BigDecimal("15000.00"));
        Factura factura3 = crearFacturaConTotal(cliente1, new BigDecimal("8000.00"));
        
        factura2.setEstado(EstadoFactura.ANULADA); // Esta no debería contarse
        
        lote.agregarFactura(factura1);
        lote.agregarFactura(factura2);
        lote.agregarFactura(factura3);

        // Act
        BigDecimal totalActivo = lote.getMontoTotalActivo();

        // Assert - Solo suma facturas con estado PENDIENTE (10000 + 8000 con redondeos)
        assertTrue(totalActivo.compareTo(new BigDecimal("17999.99")) > 0);
        assertTrue(totalActivo.compareTo(new BigDecimal("18000.01")) < 0);
    }

    @Test
    @DisplayName("Debería crear lote para facturación masiva de diciembre")
    void testLoteFacturacionDiciembre() {
        // Arrange & Act
        LoteFacturacion loteDiciembre = new LoteFacturacion(
            "Diciembre 2025",
            LocalDate.of(2025, 12, 1),
            LocalDate.of(2025, 12, 31)
        );

        // Assert
        assertEquals("Diciembre 2025", loteDiciembre.getPeriodo());
        assertEquals(LocalDate.of(2025, 12, 1), loteDiciembre.getPeriodoFecha());
        assertEquals(LocalDate.of(2025, 12, 31), loteDiciembre.getFechaVencimiento());
    }

    @Test
    @DisplayName("Debería manejar lote con muchas facturas")
    void testLoteConMuchasFacturas() {
        // Arrange - Simular facturación masiva real con 50 clientes
        BigDecimal totalEsperado = BigDecimal.ZERO;

        // Act
        for (int i = 1; i <= 50; i++) {
            CuentaCliente cliente = mock(CuentaCliente.class);
            when(cliente.getId()).thenReturn((long) i);
            when(cliente.getNombre()).thenReturn("Cliente " + i);
            when(cliente.getCondicionIva()).thenReturn(TipoCondicionIVA.RESPONSABLE_INSCRIPTO);
            when(cliente.getEstado()).thenReturn(EstadoCuenta.ACTIVA);
            
            BigDecimal montoFactura = new BigDecimal(10000 + (i * 100)); // Montos variados
            Factura factura = crearFacturaConTotal(cliente, montoFactura);
            lote.agregarFactura(factura);
            totalEsperado = totalEsperado.add(factura.getTotal()); // Sumar el total REAL de la factura
        }

        // Assert
        assertEquals(50, lote.getCantidadFacturas());
        assertTrue(lote.getMontoTotal().compareTo(totalEsperado) == 0);
    }

    @Test
    @DisplayName("Debería mantener referencia bidireccional con facturas")
    void testReferenciaBidireccional() {
        // Arrange
        Factura factura = crearFacturaConTotal(cliente1, new BigDecimal("10000.00"));

        // Act
        lote.agregarFactura(factura);

        // Assert
        assertEquals(lote, factura.getLoteFacturacion());
        assertTrue(lote.getFacturas().contains(factura));
    }

    // Método helper para crear facturas con totales calculados
    private Factura crearFacturaConTotal(CuentaCliente cliente, BigDecimal total) {
        Factura factura = new Factura(
            1,
            (int) (Math.random() * 10000),
            cliente,
            LocalDate.of(2025, 11, 1),
            LocalDate.of(2025, 11, 30),
            LocalDate.of(2025, 11, 1),
            TipoFactura.A
        );
        
        // Crear item que resulte en el total deseado
        // Total = Subtotal + IVA
        // Si IVA es 21%: Total = Subtotal * 1.21
        // Entonces: Subtotal = Total / 1.21
        BigDecimal subtotal = total.divide(new BigDecimal("1.21"), 2, java.math.RoundingMode.HALF_UP);
        
        ItemFactura item = new ItemFactura(
            "Servicio Mensual",
            subtotal,
            1,
            TipoAlicuotaIVA.IVA_21
        );
        
        factura.agregarItem(item);
        return factura;
    }
}
