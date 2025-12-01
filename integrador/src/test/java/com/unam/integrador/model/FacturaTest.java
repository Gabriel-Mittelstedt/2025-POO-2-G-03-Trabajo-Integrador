package com.unam.integrador.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.unam.integrador.model.enums.EstadoCuenta;
import com.unam.integrador.model.enums.EstadoFactura;
import com.unam.integrador.model.enums.TipoAlicuotaIVA;
import com.unam.integrador.model.enums.TipoCondicionIVA;
import com.unam.integrador.model.enums.TipoFactura;

/**
 * Tests unitarios para Factura.
 * Valida lógica de negocio: cálculos, estados, validaciones y anulaciones.
 */
@DisplayName("Tests unitarios para Factura")
class FacturaTest {

    private CuentaCliente cliente;
    private Factura factura;

    @BeforeEach
    void setUp() {
        // Crear cliente mock con datos realistas
        cliente = mock(CuentaCliente.class);
        when(cliente.getId()).thenReturn(1L);
        when(cliente.getNombre()).thenReturn("Tecnología Global S.A.");
        when(cliente.getCondicionIva()).thenReturn(TipoCondicionIVA.RESPONSABLE_INSCRIPTO);
        when(cliente.getEstado()).thenReturn(EstadoCuenta.ACTIVA);

        // Crear factura tipo A (RI a RI)
        factura = new Factura(
            1, // Serie A
            123,
            cliente,
            LocalDate.of(2025, 11, 1),
            LocalDate.of(2025, 11, 30),
            LocalDate.of(2025, 11, 1),
            TipoFactura.A
        );
    }

    @Test
    @DisplayName("Debería crear factura con valores iniciales correctos")
    void testCrearFactura() {
        // Assert
        assertEquals(1, factura.getSerie());
        assertEquals(123, factura.getNroFactura());
        assertEquals(cliente, factura.getCliente());
        assertEquals(LocalDate.of(2025, 11, 1), factura.getFechaEmision());
        assertEquals(LocalDate.of(2025, 11, 30), factura.getFechaVencimiento());
        assertEquals(LocalDate.of(2025, 11, 1), factura.getPeriodo());
        assertEquals(TipoFactura.A, factura.getTipo());
        assertEquals(EstadoFactura.PENDIENTE, factura.getEstado());
        assertEquals(BigDecimal.ZERO, factura.getSubtotal());
        assertEquals(BigDecimal.ZERO, factura.getTotal());
    }

    @Test
    @DisplayName("Debería normalizar período al día 1 del mes")
    void testNormalizarPeriodo() {
        // Arrange
        LocalDate periodoConDia15 = LocalDate.of(2025, 11, 15);

        // Act
        factura.setPeriodo(periodoConDia15);

        // Assert
        assertEquals(LocalDate.of(2025, 11, 1), factura.getPeriodo());
    }

    @Test
    @DisplayName("Debería agregar item y recalcular totales")
    void testAgregarItem() {
        // Arrange
        ItemFactura item = new ItemFactura(
            "Hosting Web Premium",
            new BigDecimal("15000.00"),
            1,
            TipoAlicuotaIVA.IVA_21
        );

        // Act
        factura.agregarItem(item);

        // Assert
        assertEquals(1, factura.getDetalleFactura().size());
        assertEquals(0, new BigDecimal("15000.00").compareTo(factura.getSubtotal()));
        assertEquals(0, new BigDecimal("3150.00").compareTo(factura.getTotalIva()));
        assertEquals(0, new BigDecimal("18150.00").compareTo(factura.getTotal()));
    }

    @Test
    @DisplayName("Debería agregar múltiples items y calcular totales correctamente")
    void testAgregarMultiplesItems() {
        // Arrange
        ItemFactura item1 = new ItemFactura(
            "Hosting Web",
            new BigDecimal("10000.00"),
            1,
            TipoAlicuotaIVA.IVA_21
        );
        ItemFactura item2 = new ItemFactura(
            "Soporte Técnico",
            new BigDecimal("5000.00"),
            1,
            TipoAlicuotaIVA.IVA_21
        );

        // Act
        factura.agregarItem(item1);
        factura.agregarItem(item2);

        // Assert
        assertEquals(2, factura.getDetalleFactura().size());
        assertEquals(0, new BigDecimal("15000.00").compareTo(factura.getSubtotal()));
        assertEquals(0, new BigDecimal("3150.00").compareTo(factura.getTotalIva()));
        assertEquals(0, new BigDecimal("18150.00").compareTo(factura.getTotal()));
    }

    @Test
    @DisplayName("Debería aplicar descuento del 10% correctamente")
    void testAplicarDescuento10Porciento() {
        // Arrange
        ItemFactura item = new ItemFactura(
            "Servicio Cloud",
            new BigDecimal("10000.00"),
            1,
            TipoAlicuotaIVA.IVA_21
        );
        factura.agregarItem(item);

        // Act
        factura.aplicarDescuento(10.0, "Descuento por volumen");

        // Assert
        assertEquals(10.0, factura.getDescuento());
        assertEquals("Descuento por volumen", factura.getMotivoDescuento());
        
        // Subtotal: 10000
        // Descuento: 1000 (10%)
        // Base: 9000
        // IVA: 9000 * 0.21 = 1890 (se calcula sobre el subtotal original)
        // Total: 9000 + 2100 = 11100
        assertEquals(0, new BigDecimal("1000.00").compareTo(factura.getMontoDescuento()));
        assertEquals(0, new BigDecimal("11100.00").compareTo(factura.getTotal()));
    }

    @Test
    @DisplayName("Debería lanzar excepción al aplicar descuento sin motivo")
    void testAplicarDescuentoSinMotivo() {
        // Arrange
        ItemFactura item = new ItemFactura("Servicio", new BigDecimal("10000"), 1, TipoAlicuotaIVA.IVA_21);
        factura.agregarItem(item);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> factura.aplicarDescuento(10.0, null)
        );
        assertEquals("El motivo del descuento es obligatorio", exception.getMessage());
    }

    @Test
    @DisplayName("Debería lanzar excepción al aplicar descuento mayor a 100%")
    void testAplicarDescuentoMayorA100() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> factura.aplicarDescuento(150.0, "Descuento inválido")
        );
        assertEquals("El porcentaje debe estar entre 0 y 100", exception.getMessage());
    }

    @Test
    @DisplayName("Debería validar fechas correctamente")
    void testValidarFechasCorrectas() {
        // Act & Assert - No debería lanzar excepción
        assertDoesNotThrow(() -> factura.validarFechas());
    }

    @Test
    @DisplayName("Debería lanzar excepción cuando vencimiento es anterior a emisión")
    void testValidarFechasVencimientoAnterior() {
        // Arrange
        Factura facturaInvalida = new Factura(
            1, 124, cliente,
            LocalDate.of(2025, 11, 30),
            LocalDate.of(2025, 11, 20), // Vencimiento anterior a emisión
            LocalDate.of(2025, 11, 1),
            TipoFactura.A
        );

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> facturaInvalida.validarFechas()
        );
        assertTrue(exception.getMessage().contains("debe ser posterior"));
    }

    @Test
    @DisplayName("Debería validar cliente activo correctamente")
    void testValidarClienteActivo() {
        // Act & Assert
        assertDoesNotThrow(() -> factura.validarClienteActivo());
    }

    @Test
    @DisplayName("Debería lanzar excepción cuando cliente está suspendido")
    void testValidarClienteSuspendido() {
        // Arrange
        when(cliente.getEstado()).thenReturn(EstadoCuenta.SUSPENDIDA);

        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> factura.validarClienteActivo()
        );
        assertTrue(exception.getMessage().contains("no tiene cuenta activa"));
    }

    @Test
    @DisplayName("Debería determinar tipo A cuando ambos son RI")
    void testDeterminarTipoFacturaA() {
        // Act
        TipoFactura tipo = Factura.determinarTipoFactura(
            TipoCondicionIVA.RESPONSABLE_INSCRIPTO,
            TipoCondicionIVA.RESPONSABLE_INSCRIPTO
        );

        // Assert
        assertEquals(TipoFactura.A, tipo);
    }

    @Test
    @DisplayName("Debería determinar tipo B cuando emisor es RI y cliente es consumidor final")
    void testDeterminarTipoFacturaB() {
        // Act
        TipoFactura tipo = Factura.determinarTipoFactura(
            TipoCondicionIVA.RESPONSABLE_INSCRIPTO,
            TipoCondicionIVA.CONSUMIDOR_FINAL
        );

        // Assert
        assertEquals(TipoFactura.B, tipo);
    }

    @Test
    @DisplayName("Debería determinar tipo C cuando emisor es monotributista")
    void testDeterminarTipoFacturaC() {
        // Act
        TipoFactura tipo = Factura.determinarTipoFactura(
            TipoCondicionIVA.MONOTRIBUTISTA,
            TipoCondicionIVA.RESPONSABLE_INSCRIPTO
        );

        // Assert
        assertEquals(TipoFactura.C, tipo);
    }

    @Test
    @DisplayName("Debería actualizar estado a VENCIDA cuando corresponde")
    void testActualizarSiEstaVencida() {
        // Arrange - Factura con vencimiento pasado
        Factura facturaVencida = new Factura(
            1, 125, cliente,
            LocalDate.of(2025, 10, 1),
            LocalDate.of(2025, 10, 31), // Vencida
            LocalDate.of(2025, 10, 1),
            TipoFactura.A
        );
        ItemFactura item = new ItemFactura("Servicio", new BigDecimal("10000"), 1, TipoAlicuotaIVA.IVA_21);
        facturaVencida.agregarItem(item);

        // Act
        boolean actualizada = facturaVencida.actualizarSiEstaVencida();

        // Assert
        assertTrue(actualizada);
        assertEquals(EstadoFactura.VENCIDA, facturaVencida.getEstado());
    }

    @Test
    @DisplayName("No debería actualizar a VENCIDA cuando aún no venció")
    void testNoActualizarSiNoVencio() {
        // Arrange - Factura con vencimiento futuro
        Factura facturaFutura = new Factura(
            1, 126, cliente,
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            LocalDate.now(),
            TipoFactura.A
        );
        ItemFactura item = new ItemFactura("Servicio", new BigDecimal("10000"), 1, TipoAlicuotaIVA.IVA_21);
        facturaFutura.agregarItem(item);

        // Act
        boolean actualizada = facturaFutura.actualizarSiEstaVencida();

        // Assert
        assertFalse(actualizada);
        assertEquals(EstadoFactura.PENDIENTE, facturaFutura.getEstado());
    }

    @Test
    @DisplayName("Debería validar que factura PENDIENTE puede ser anulada")
    void testPuedeSerAnuladaPendiente() {
        // Assert
        assertTrue(factura.puedeSerAnulada());
    }

    @Test
    @DisplayName("Debería validar que factura VENCIDA puede ser anulada")
    void testPuedeSerAnuladaVencida() {
        // Arrange
        factura.actualizarSiEstaVencida(); // Simular que está vencida

        // Assert
        assertTrue(factura.puedeSerAnulada());
    }


    @Test
    @DisplayName("Debería anular factura correctamente")
    void testAnularFactura() {
        // Act
        factura.anular();

        // Assert
        assertEquals(EstadoFactura.ANULADA, factura.getEstado());
    }


    @Test
    @DisplayName("Debería obtener período formateado correctamente")
    void testGetPeriodoFormateado() {
        // Act
        String periodo = factura.getPeriodoFormateado();

        // Assert
        assertEquals("Noviembre 2025", periodo);
    }

    @Test
    @DisplayName("Debería lanzar excepción al agregar item nulo")
    void testAgregarItemNulo() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> factura.agregarItem(null)
        );
        assertEquals("El item no puede ser nulo", exception.getMessage());
    }

    @Test
    @DisplayName("Debería calcular totales en el orden correcto")
    void testCalcularTotales() {
        // Arrange
        ItemFactura item1 = new ItemFactura("Hosting", new BigDecimal("10000"), 1, TipoAlicuotaIVA.IVA_21);
        ItemFactura item2 = new ItemFactura("Email", new BigDecimal("5000"), 1, TipoAlicuotaIVA.IVA_21);
        factura.agregarItem(item1);
        factura.agregarItem(item2);

        // Act
        factura.calcularTotales();

        // Assert
        assertEquals(0, new BigDecimal("15000.00").compareTo(factura.getSubtotal()));
        assertEquals(0, new BigDecimal("3150.00").compareTo(factura.getTotalIva()));
        assertEquals(0, new BigDecimal("18150.00").compareTo(factura.getTotal()));
        assertEquals(0, new BigDecimal("18150.00").compareTo(factura.getSaldoPendiente()));
    }
}
