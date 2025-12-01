package com.unam.integrador.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.unam.integrador.model.enums.TipoAlicuotaIVA;

/**
 * Tests unitarios para ItemFactura.
 * Valida cálculos de subtotal, IVA, total y facturación proporcional.
 */
@DisplayName("Tests unitarios para ItemFactura")
class ItemFacturaTest {

    private ItemFactura itemFactura;

    @BeforeEach
    void setUp() {
        // Item típico: Hosting Web $15000 con IVA 21%
        itemFactura = new ItemFactura(
            "Hosting Web - Plan Premium",
            new BigDecimal("15000.00"),
            1,
            TipoAlicuotaIVA.IVA_21
        );
    }

    @Test
    @DisplayName("Debería crear item con valores iniciales correctos")
    void testCrearItem() {
        // Assert
        assertEquals("Hosting Web - Plan Premium", itemFactura.getDescripcion());
        assertEquals(0, new BigDecimal("15000.00").compareTo(itemFactura.getPrecioUnitario()));
        assertEquals(1, itemFactura.getCantidad());
        assertEquals(TipoAlicuotaIVA.IVA_21, itemFactura.getAlicuotaIVA());
        assertEquals(BigDecimal.ZERO, itemFactura.getSubtotal());
        assertEquals(BigDecimal.ZERO, itemFactura.getMontoIva());
        assertEquals(BigDecimal.ZERO, itemFactura.getTotal());
    }

    @Test
    @DisplayName("Debería calcular subtotal correctamente con cantidad 1")
    void testCalcularSubtotalCantidad1() {
        // Act
        BigDecimal subtotal = itemFactura.calcularSubtotal();

        // Assert
        assertEquals(0, new BigDecimal("15000.00").compareTo(subtotal));
    }

    @Test
    @DisplayName("Debería calcular subtotal correctamente con cantidad múltiple")
    void testCalcularSubtotalCantidadMultiple() {
        // Arrange - 3 licencias de software a $5000 cada una
        ItemFactura item = new ItemFactura(
            "Licencia Microsoft Office",
            new BigDecimal("5000.00"),
            3,
            TipoAlicuotaIVA.IVA_21
        );

        // Act
        BigDecimal subtotal = item.calcularSubtotal();

        // Assert
        assertEquals(0, new BigDecimal("15000.00").compareTo(subtotal));
    }

    @Test
    @DisplayName("Debería calcular IVA 21% correctamente")
    void testCalcularIVA21() {
        // Arrange
        itemFactura.calcularSubtotal();

        // Act
        BigDecimal iva = itemFactura.calcularMontoIva();

        // Assert - 15000 * 0.21 = 3150
        assertEquals(0, new BigDecimal("3150.00").compareTo(iva));
    }

    @Test
    @DisplayName("Debería calcular IVA 10.5% correctamente")
    void testCalcularIVA10_5() {
        // Arrange - Servicio de Internet con IVA reducido
        ItemFactura item = new ItemFactura(
            "Servicio de Internet Fibra Óptica",
            new BigDecimal("8000.00"),
            1,
            TipoAlicuotaIVA.IVA_10_5
        );
        item.calcularSubtotal();

        // Act
        BigDecimal iva = item.calcularMontoIva();

        // Assert - 8000 * 0.105 = 840
        assertEquals(0, new BigDecimal("840.00").compareTo(iva));
    }

    @Test
    @DisplayName("Debería calcular IVA 27% correctamente")
    void testCalcularIVA27() {
        // Arrange - Servicios de telecomunicaciones
        ItemFactura item = new ItemFactura(
            "Telefonía Móvil Empresarial",
            new BigDecimal("10000.00"),
            1,
            TipoAlicuotaIVA.IVA_27
        );
        item.calcularSubtotal();

        // Act
        BigDecimal iva = item.calcularMontoIva();

        // Assert - 10000 * 0.27 = 2700
        assertEquals(0, new BigDecimal("2700.00").compareTo(iva));
    }

    @Test
    @DisplayName("Debería calcular IVA exento correctamente")
    void testCalcularIVAExento() {
        // Arrange - Servicios educativos (exentos)
        ItemFactura item = new ItemFactura(
            "Capacitación en Tecnología",
            new BigDecimal("12000.00"),
            1,
            TipoAlicuotaIVA.EXENTO
        );
        item.calcularSubtotal();

        // Act
        BigDecimal iva = item.calcularMontoIva();

        // Assert
        assertEquals(0, BigDecimal.ZERO.compareTo(iva));
    }

    @Test
    @DisplayName("Debería calcular total correctamente")
    void testCalcularTotal() {
        // Arrange
        itemFactura.calcularSubtotal();
        itemFactura.calcularMontoIva();

        // Act
        BigDecimal total = itemFactura.calcularTotal();

        // Assert - 15000 + 3150 = 18150
        assertEquals(0, new BigDecimal("18150.00").compareTo(total));
    }

    @Test
    @DisplayName("Debería ejecutar todos los cálculos con calcular()")
    void testCalcularTodo() {
        // Act
        itemFactura.calcular();

        // Assert
        assertEquals(0, new BigDecimal("15000.00").compareTo(itemFactura.getSubtotal()));
        assertEquals(0, new BigDecimal("3150.00").compareTo(itemFactura.getMontoIva()));
        assertEquals(0, new BigDecimal("18150.00").compareTo(itemFactura.getTotal()));
    }

    @Test
    @DisplayName("Debería crear item proporcional correctamente - 16 días de 30")
    void testCrearItemProporcional16De30Dias() {
        // Arrange
        LocalDate inicio = LocalDate.of(2025, 11, 15);
        LocalDate fin = LocalDate.of(2025, 11, 30);
        PeriodoFacturacion periodo = new PeriodoFacturacion(inicio, fin);

        // Act
        ItemFactura item = ItemFactura.crearProporcional(
            "Hosting Web Premium",
            new BigDecimal("15000.00"),
            1,
            TipoAlicuotaIVA.IVA_21,
            periodo
        );

        // Assert
        assertTrue(item.getDescripcion().contains("Hosting Web Premium"));
        assertTrue(item.getDescripcion().contains("15 al 30 de Noviembre 2025"));
        
        // Precio proporcional: 15000 * (16/30 con 4 decimales) = 15000 * 0.5333 = 7999.50
        assertEquals(0, new BigDecimal("7999.50").compareTo(item.getPrecioUnitario()));
    }

    @Test
    @DisplayName("Debería crear item proporcional correctamente - 20 días de 31")
    void testCrearItemProporcional20De31Dias() {
        // Arrange - Cliente dado de baja el 20 de diciembre
        LocalDate inicio = LocalDate.of(2025, 12, 1);
        LocalDate fin = LocalDate.of(2025, 12, 20);
        PeriodoFacturacion periodo = new PeriodoFacturacion(inicio, fin);

        // Act
        ItemFactura item = ItemFactura.crearProporcional(
            "Servicio Cloud Computing",
            new BigDecimal("31000.00"),
            1,
            TipoAlicuotaIVA.IVA_21,
            periodo
        );

        // Assert
        assertTrue(item.getDescripcion().contains("1 al 20 de Diciembre 2025"));
        
        // Precio proporcional: 31000 * (20/31 con 4 decimales) = 31000 * 0.6452 = 20001.20
        assertEquals(0, new BigDecimal("20001.20").compareTo(item.getPrecioUnitario()));
    }

    @Test
    @DisplayName("Debería calcular total correcto en facturación proporcional")
    void testCalcularTotalFacturacionProporcional() {
        // Arrange
        LocalDate inicio = LocalDate.of(2025, 11, 15);
        LocalDate fin = LocalDate.of(2025, 11, 30);
        PeriodoFacturacion periodo = new PeriodoFacturacion(inicio, fin);
        
        ItemFactura item = ItemFactura.crearProporcional(
            "Hosting Web",
            new BigDecimal("15000.00"),
            1,
            TipoAlicuotaIVA.IVA_21,
            periodo
        );

        // Act
        item.calcular();

        // Assert
        // Precio proporcional: 7999.50
        // IVA: 7999.50 * 0.21 = 1679.895
        // Total: 7999.50 + 1679.895 = 9679.395
        assertEquals(0, new BigDecimal("7999.50").compareTo(item.getSubtotal()));
        assertEquals(0, new BigDecimal("1679.895").compareTo(item.getMontoIva()));
        assertEquals(0, new BigDecimal("9679.395").compareTo(item.getTotal()));
    }

    @Test
    @DisplayName("Debería manejar precios con decimales correctamente")
    void testPreciosConDecimales() {
        // Arrange - Servicio con precio decimal
        ItemFactura item = new ItemFactura(
            "Servicio Premium Plus",
            new BigDecimal("12345.67"),
            1,
            TipoAlicuotaIVA.IVA_21
        );

        // Act
        item.calcular();

        // Assert - IVA: 12345.67 * 0.21 = 2592.5907
        assertEquals(0, new BigDecimal("12345.67").compareTo(item.getSubtotal()));
        assertEquals(0, new BigDecimal("2592.5907").compareTo(item.getMontoIva()));
        assertEquals(0, new BigDecimal("14938.2607").compareTo(item.getTotal()));
    }

    @Test
    @DisplayName("Debería calcular correctamente con IVA 2.5%")
    void testCalcularIVA2_5() {
        // Arrange - Algunos servicios tienen IVA reducido del 2.5%
        ItemFactura item = new ItemFactura(
            "Servicio Especial",
            new BigDecimal("10000.00"),
            1,
            TipoAlicuotaIVA.IVA_2_5
        );

        // Act
        item.calcular();

        // Assert - 10000 * 0.025 = 250
        assertEquals(0, new BigDecimal("10000.00").compareTo(item.getSubtotal()));
        assertEquals(0, new BigDecimal("250.00").compareTo(item.getMontoIva()));
        assertEquals(0, new BigDecimal("10250.00").compareTo(item.getTotal()));
    }

    @Test
    @DisplayName("Debería crear item proporcional de un solo día")
    void testItemProporcionalUnDia() {
        // Arrange
        LocalDate fecha = LocalDate.of(2025, 11, 30);
        PeriodoFacturacion periodo = new PeriodoFacturacion(fecha, fecha);

        // Act
        ItemFactura item = ItemFactura.crearProporcional(
            "Hosting VPS",
            new BigDecimal("30000.00"),
            1,
            TipoAlicuotaIVA.IVA_21,
            periodo
        );

        // Assert - 30000 * (1/30 con 4 decimales) = 30000 * 0.0333 = 999.00
        assertEquals(0, new BigDecimal("999.00").compareTo(item.getPrecioUnitario()));
    }
}
