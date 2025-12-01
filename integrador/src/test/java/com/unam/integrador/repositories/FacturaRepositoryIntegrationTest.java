package com.unam.integrador.repositories;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.unam.integrador.model.*;
import com.unam.integrador.model.enums.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración para FacturaRepository.
 * Valida queries personalizadas y consultas específicas usando base de datos H2.
 */
@DataJpaTest
@DisplayName("Tests de integración para FacturaRepository")
class FacturaRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private FacturaRepository facturaRepository;

    private CuentaCliente cliente1;
    private CuentaCliente cliente2;
    private Factura factura1;
    private Factura factura2;
    private Factura factura3;

    @BeforeEach
    void setUp() {
        // Crear clientes
        cliente1 = crearCliente("Global Tech S.A.", "30-12345678-9", TipoCondicionIVA.RESPONSABLE_INSCRIPTO);
        cliente2 = crearCliente("Innovate Corp.", "30-87654321-0", TipoCondicionIVA.RESPONSABLE_INSCRIPTO);

        // Crear facturas para cliente1
        factura1 = crearFactura(cliente1, 1, 100, LocalDate.of(2025, 11, 1), 
            LocalDate.of(2025, 11, 30), TipoFactura.A, EstadoFactura.PENDIENTE);
        
        factura2 = crearFactura(cliente1, 1, 101, LocalDate.of(2025, 10, 1), 
            LocalDate.of(2025, 10, 31), TipoFactura.A, EstadoFactura.VENCIDA);
        
        // Crear factura para cliente2
        factura3 = crearFactura(cliente2, 1, 102, LocalDate.of(2025, 11, 1), 
            LocalDate.of(2025, 11, 30), TipoFactura.A, EstadoFactura.PENDIENTE);
        
        entityManager.flush();
    }

    @Test
    @DisplayName("Debería buscar facturas por cliente")
    void testFindByClienteId() {
        // Act
        List<Factura> facturas = facturaRepository.findByClienteId(cliente1.getId());

        // Assert
        assertEquals(2, facturas.size());
        assertTrue(facturas.stream().allMatch(f -> f.getCliente().equals(cliente1)));
    }

    @Test
    @DisplayName("Debería buscar facturas por estado")
    void testFindByEstado() {
        // Act
        List<Factura> pendientes = facturaRepository.findByEstado(EstadoFactura.PENDIENTE);

        // Assert
        assertEquals(2, pendientes.size());
        assertTrue(pendientes.stream().allMatch(f -> f.getEstado() == EstadoFactura.PENDIENTE));
    }

    @Test
    @DisplayName("Debería buscar facturas por tipo")
    void testFindByTipo() {
        // Arrange - Crear factura tipo B
        CuentaCliente consumidorFinal = crearCliente("Juan Pérez", "20-12345678-9", TipoCondicionIVA.CONSUMIDOR_FINAL);
        Factura facturaB = crearFactura(consumidorFinal, 2, 50, LocalDate.of(2025, 11, 1),
            LocalDate.of(2025, 11, 30), TipoFactura.B, EstadoFactura.PENDIENTE);
        entityManager.flush();

        // Act
        List<Factura> facturasA = facturaRepository.findByTipo(TipoFactura.A);
        List<Factura> facturasB = facturaRepository.findByTipo(TipoFactura.B);

        // Assert
        assertEquals(3, facturasA.size());
        assertEquals(1, facturasB.size());
    }

    @Test
    @DisplayName("Debería buscar facturas por período")
    void testFindByPeriodo() {
        // Act
        LocalDate periodoNoviembre = LocalDate.of(2025, 11, 1);
        List<Factura> facturas = facturaRepository.findByPeriodo(periodoNoviembre);

        // Assert
        assertEquals(2, facturas.size());
        assertTrue(facturas.stream().allMatch(f -> f.getPeriodo().equals(periodoNoviembre)));
    }

    @Test
    @DisplayName("Debería buscar facturas por cliente y estado")
    void testFindByClienteIdAndEstado() {
        // Act
        List<Factura> facturas = facturaRepository.findByClienteIdAndEstado(
            cliente1.getId(), 
            EstadoFactura.PENDIENTE
        );

        // Assert
        assertEquals(1, facturas.size());
        assertEquals(factura1.getIdFactura(), facturas.get(0).getIdFactura());
    }

    @Test
    @DisplayName("Debería obtener último número de factura por serie")
    void testFindFirstBySerieOrderByNroFacturaDesc() {
        // Arrange - Crear más facturas en serie 1
        crearFactura(cliente1, 1, 200, LocalDate.of(2025, 12, 1),
            LocalDate.of(2025, 12, 31), TipoFactura.A, EstadoFactura.PENDIENTE);
        entityManager.flush();

        // Act
        Factura ultimaFactura = facturaRepository.findFirstBySerieOrderByNroFacturaDesc(1);

        // Assert
        assertNotNull(ultimaFactura);
        assertEquals(200, ultimaFactura.getNroFactura());
    }

    @Test
    @DisplayName("Debería retornar null si no hay facturas en la serie")
    void testFindFirstBySerieOrderByNroFacturaDesc_NoFacturas() {
        // Act
        Factura ultimaFactura = facturaRepository.findFirstBySerieOrderByNroFacturaDesc(5);

        // Assert
        assertNull(ultimaFactura);
    }

    @Test
    @DisplayName("Debería buscar factura por serie y número")
    void testFindBySerieAndNroFactura() {
        // Act
        Optional<Factura> factura = facturaRepository.findBySerieAndNroFactura(1, 100);

        // Assert
        assertTrue(factura.isPresent());
        assertEquals(factura1.getIdFactura(), factura.get().getIdFactura());
    }

    @Test
    @DisplayName("Debería buscar facturas impagas por cliente")
    void testFindByClienteIdAndEstadoInOrderByFechaEmisionAsc() {
        // Arrange - Crear facturas con diferentes estados
        Factura facturaPagada = crearFactura(cliente1, 1, 103, LocalDate.of(2025, 9, 1),
            LocalDate.of(2025, 9, 30), TipoFactura.A, EstadoFactura.PAGADA_TOTALMENTE);
        entityManager.flush();

        List<EstadoFactura> estadosImpagas = List.of(
            EstadoFactura.PENDIENTE,
            EstadoFactura.VENCIDA,
            EstadoFactura.PAGADA_PARCIALMENTE
        );

        // Act
        List<Factura> facturasImpagas = facturaRepository
            .findByClienteIdAndEstadoInOrderByFechaEmisionAsc(cliente1.getId(), estadosImpagas);

        // Assert
        assertEquals(2, facturasImpagas.size());
        // Verificar que están ordenadas por fecha de emisión ascendente
        assertTrue(facturasImpagas.get(0).getFechaEmision()
            .isBefore(facturasImpagas.get(1).getFechaEmision()) ||
            facturasImpagas.get(0).getFechaEmision()
            .isEqual(facturasImpagas.get(1).getFechaEmision()));
    }

    @Test
    @DisplayName("Debería verificar existencia de factura por cliente y período (excluyendo anuladas)")
    void testExistsByClienteIdAndPeriodoAndEstadoNot() {
        // Act
        boolean existe = facturaRepository.existsByClienteIdAndPeriodoAndEstadoNot(
            cliente1.getId(),
            LocalDate.of(2025, 11, 1),
            EstadoFactura.ANULADA
        );

        // Assert
        assertTrue(existe);
    }

    @Test
    @DisplayName("No debería encontrar factura anulada en verificación de existencia")
    void testExistsByClienteIdAndPeriodoAndEstadoNot_FacturaAnulada() {
        // Arrange - Anular la factura existente
        factura1.setEstado(EstadoFactura.ANULADA);
        entityManager.merge(factura1);
        entityManager.flush();

        // Act
        boolean existe = facturaRepository.existsByClienteIdAndPeriodoAndEstadoNot(
            cliente1.getId(),
            LocalDate.of(2025, 11, 1),
            EstadoFactura.ANULADA
        );

        // Assert
        assertFalse(existe);
    }

    @Test
    @DisplayName("Debería persistir y recuperar factura con items")
    void testPersistirFacturaConItems() {
        // Arrange - Crear factura sin items iniciales
        Factura nuevaFactura = new Factura(
            1,
            999,
            cliente1,
            LocalDate.of(2025, 12, 1),
            LocalDate.of(2025, 12, 31),
            LocalDate.of(2025, 12, 1), // período
            TipoFactura.A
        );
        nuevaFactura.setEstado(EstadoFactura.PENDIENTE);
        
        ItemFactura item1 = new ItemFactura("Hosting Web", new BigDecimal("10000"), 1, TipoAlicuotaIVA.IVA_21);
        ItemFactura item2 = new ItemFactura("Email", new BigDecimal("5000"), 1, TipoAlicuotaIVA.IVA_21);
        
        nuevaFactura.agregarItem(item1);
        nuevaFactura.agregarItem(item2);
        
        // Act
        Factura guardada = facturaRepository.save(nuevaFactura);
        entityManager.flush();
        entityManager.clear(); // Limpiar caché

        Factura recuperada = facturaRepository.findById(guardada.getIdFactura()).orElse(null);

        // Assert
        assertNotNull(recuperada);
        assertEquals(2, recuperada.getDetalleFactura().size());
        assertEquals(0, new BigDecimal("15000.00").compareTo(recuperada.getSubtotal()));
    }

    @Test
    @DisplayName("Debería eliminar factura correctamente")
    void testEliminarFactura() {
        // Act
        Long id = factura1.getIdFactura();
        facturaRepository.deleteById(id);
        entityManager.flush();

        Optional<Factura> eliminada = facturaRepository.findById(id);

        // Assert
        assertFalse(eliminada.isPresent());
    }

    @Test
    @DisplayName("Debería contar facturas por estado")
    void testContarFacturasPorEstado() {
        // Act
        List<Factura> pendientes = facturaRepository.findByEstado(EstadoFactura.PENDIENTE);
        List<Factura> vencidas = facturaRepository.findByEstado(EstadoFactura.VENCIDA);

        // Assert
        assertEquals(2, pendientes.size());
        assertEquals(1, vencidas.size());
    }

    // Métodos helper

    private CuentaCliente crearCliente(String nombre, String cuit, TipoCondicionIVA condicionIva) {
        CuentaCliente cliente = new CuentaCliente();
        cliente.setNombre(nombre);
        cliente.setRazonSocial(nombre);
        // Asegurar que el CUIT tenga 11 dígitos
        String cuitLimpio = cuit.replace("-", "");
        if (cuitLimpio.length() < 11) {
            cuitLimpio = cuitLimpio + "00000000000".substring(cuitLimpio.length());
        }
        cliente.setCuitDni(cuitLimpio.substring(0, 11));
        cliente.setDomicilio("Av. Corrientes 1234");
        cliente.setTelefono("11-1234-5678");
        cliente.setEmail(nombre.toLowerCase().replace(" ", "").replace(".", "") + "@email.com");
        cliente.setCondicionIva(condicionIva);
        cliente.setEstado(EstadoCuenta.ACTIVA);
        return entityManager.persist(cliente);
    }

    private Factura crearFactura(CuentaCliente cliente, int serie, int numero, 
                                  LocalDate fechaEmision, LocalDate fechaVencimiento,
                                  TipoFactura tipo, EstadoFactura estado) {
        Factura factura = new Factura(
            serie,
            numero,
            cliente,
            fechaEmision,
            fechaVencimiento,
            fechaEmision, // período = fecha de emisión
            tipo
        );
        factura.setEstado(estado);
        
        // Agregar un item para que tenga datos válidos
        ItemFactura item = new ItemFactura(
            "Servicio de prueba",
            new BigDecimal("10000"),
            1,
            TipoAlicuotaIVA.IVA_21
        );
        factura.agregarItem(item);
        
        return entityManager.persist(factura);
    }
}
