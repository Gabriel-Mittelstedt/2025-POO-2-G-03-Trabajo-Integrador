package com.unam.integrador.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.unam.integrador.model.enums.EstadoCuenta;
import com.unam.integrador.model.enums.EstadoFactura;
import com.unam.integrador.model.enums.TipoCondicionIVA;
import com.unam.integrador.model.enums.TipoFactura;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Entidad que representa una factura en el sistema.
 * Modelo RICO: encapsula toda la lógica de negocio relacionada con facturas,
 * incluyendo cálculos, validaciones y cambios de estado.
 * 
 * Responsabilidades:
 * - Gestionar items de facturación
 * - Calcular totales, IVA y descuentos
 * - Controlar estados (PENDIENTE, VENCIDA, PAGADA, ANULADA)
 * - Validar condiciones de negocio
 * - Registrar pagos y notas de crédito
 * 
 * @author Sistema ERP Facturación
 * @version 1.0
 */
@Data
@Entity
@NoArgsConstructor
public class Factura {
    
    /** Identificador único de la factura (clave primaria). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idFactura;

    /** Serie de la factura según el tipo (A=1, B=2, C=3). */
    @Column(nullable = false)
    private int serie;

    /** Número secuencial de la factura dentro de su serie. */
    @Column(nullable = false)
    private int nroFactura;

    /** Cliente al que se le emite la factura. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private CuentaCliente cliente;

    // --- Atributos de la Factura ---
    
    /** Fecha en que se emite la factura. */
    private LocalDate fechaEmision;
    
    /** Fecha límite para el pago de la factura. */
    private LocalDate fechaVencimiento;
    
    /**
     * Período de facturación almacenado como LocalDate.
     * Siempre usa el día 1 del mes correspondiente.
     */
    private LocalDate periodo;

    /** Tipo de factura (A, B o C) según condiciones fiscales. */
    @Enumerated(EnumType.STRING)
    private TipoFactura tipo;
    
    /** Estado actual de la factura (PENDIENTE, VENCIDA, PAGADA_PARCIALMENTE, PAGADA_TOTALMENTE, ANULADA). */
    @Enumerated(EnumType.STRING)
    //@Setter(AccessLevel.NONE)
    private EstadoFactura estado;

    // --- Campos Calculados y Opcionales ---
    
    /** Suma de todos los items sin IVA ni descuentos. */
    private BigDecimal subtotal;
    
    /** Porcentaje de descuento aplicado (0-100). */
    private double descuento;
    
    /** Justificación del descuento aplicado (obligatorio si hay descuento). */
    private String motivoDescuento;
    
    /** Suma del IVA de todos los items. */
    private BigDecimal totalIva;
    
    /** Monto que aún falta pagar de la factura. */
    private BigDecimal saldoPendiente;
    
    /** Importe final de la factura (subtotal - descuento + IVA). */
    private BigDecimal total;

    //--Relaciones--
    
    /** Líneas de detalle de la factura (servicios facturados). */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id")
    @ToString.Exclude
    private final List<ItemFactura> detalleFactura = new ArrayList<>();

    /** Notas de crédito asociadas (generadas por anulaciones). */
    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private final List<NotaCredito> notasCredito = new ArrayList<>();

    /**
     * Detalles de pago aplicados a esta factura.
     * Una factura puede tener múltiples pagos (pagos parciales).
     */
    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private final List<DetallePago> detallesPago = new ArrayList<>();

    /**
     * Lote de facturación masiva al que pertenece esta factura (si aplica).
     * Null para facturas individuales.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_facturacion_id")
    @ToString.Exclude
    private LoteFacturacion loteFacturacion;

    /**
     * Constructor principal para crear una factura.
     * Inicializa todos los campos obligatorios y establece valores por defecto.
     * 
     * @param serie Serie de la factura (1=A, 2=B, 3=C)
     * @param nroFactura Número secuencial de la factura
     * @param cliente Cliente al que se factura
     * @param fechaEmision Fecha de emisión de la factura
     * @param fechaVencimiento Fecha límite de pago
     * @param periodo Período facturado (se ajusta al día 1 del mes)
     * @param tipo Tipo de factura (A, B o C)
     */
    public Factura(int serie, int nroFactura, CuentaCliente cliente, LocalDate fechaEmision, 
                   LocalDate fechaVencimiento, LocalDate periodo, TipoFactura tipo) {
        
        this.serie = serie;
        this.nroFactura = nroFactura;
        this.cliente = cliente;
        this.fechaEmision = fechaEmision;
        this.fechaVencimiento = fechaVencimiento;
        // Asegurar que el día siempre sea 1
        this.periodo = periodo.withDayOfMonth(1);
        this.tipo = tipo;

        // --- Valores por defecto al crear una factura ---
        this.estado = EstadoFactura.PENDIENTE;
        this.subtotal = BigDecimal.ZERO;
        this.descuento = 0.0;
        this.totalIva = BigDecimal.ZERO;
        this.saldoPendiente = BigDecimal.ZERO;
        this.total = BigDecimal.ZERO;
        this.motivoDescuento = null;
    }

    // --- MÉTODOS DE NEGOCIO (Modelo RICO) ---

    /**
     * Agrega un item a la factura y recalcula los totales.
     * Este método encapsula la lógica de agregar items manteniendo la consistencia.
     * 
     * @param item El item a agregar
     */
    public void agregarItem(ItemFactura item) {
        if (item == null) {
            throw new IllegalArgumentException("El item no puede ser nulo");
        }
        
        // Calcular valores del item
        item.calcular();
        
        // Asociar el item a esta factura
        item.setFactura(this);
        
        // Agregar a la lista
        this.detalleFactura.add(item);
        
        // Recalcular totales de la factura
        calcularTotales();
    }

    /**
     * Calcula el subtotal de la factura sumando todos los items.
     * @return Subtotal antes de IVA y descuentos
     */
    public BigDecimal calcularSubtotal() {
        //convierte la lista a un stream para procesarlo de manera funcional
        this.subtotal = detalleFactura.stream()
            //obtiene el subtotal de cada item
            .map(ItemFactura::getSubtotal)
            //suma todos los subtotales obtenidos
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return this.subtotal;
    }

    /**
     * Calcula el total de IVA de la factura sumando el IVA de todos los items.
     * @return Total de IVA
     */
    public BigDecimal calcularTotalIva() {
        this.totalIva = detalleFactura.stream()
            .map(ItemFactura::getMontoIva)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return this.totalIva;
    }

    /**
     * Aplica un descuento porcentual a la factura.
     * 
     * @param porcentaje Porcentaje de descuento (0-100)
     * @param motivo Motivo del descuento
     * @throws IllegalArgumentException si el porcentaje es inválido
     */
    public void aplicarDescuento(double porcentaje, String motivo) {
        if (porcentaje < 0 || porcentaje > 100) {
            throw new IllegalArgumentException("El porcentaje debe estar entre 0 y 100");
        }
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IllegalArgumentException("El motivo del descuento es obligatorio");
        }
        
        this.descuento = porcentaje;
        this.motivoDescuento = motivo;
        
        // Recalcular total con el nuevo descuento
        calcularTotal();
        calcularSaldoPendiente();
    }

    /**
     * Calcula el total de la factura aplicando la fórmula:
     * Total = Subtotal - Descuento + IVA
     * 
     * @return Total de la factura
     */
    public BigDecimal calcularTotal() {
        // Calcular monto del descuento
        BigDecimal montoDescuento = this.subtotal
            .multiply(BigDecimal.valueOf(this.descuento))
            .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        
        // Total = (Subtotal - Descuento) + IVA
        BigDecimal subtotalConDescuento = this.subtotal.subtract(montoDescuento);
        this.total = subtotalConDescuento.add(this.totalIva);
        
        return this.total;
    }

    /**
     * Calcula el saldo pendiente de pago.
     * Inicialmente es igual al total de la factura.
     * 
     * @return Saldo pendiente
     */
    public BigDecimal calcularSaldoPendiente() {
        this.saldoPendiente = this.total;
        return this.saldoPendiente;
    }

    /**
     * Obtiene el monto del descuento aplicado.
     * Se calcula como un porcentaje del subtotal.
     * 
     * @return Monto del descuento en pesos
     */
    public BigDecimal getMontoDescuento() {
        if (this.descuento <= 0) {
            return BigDecimal.ZERO;
        }
        return this.subtotal
            .multiply(BigDecimal.valueOf(this.descuento))
            .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Ejecuta todos los cálculos en el orden correcto.
     * Este es el método principal para actualizar todos los valores calculados.
     */
    public void calcularTotales() {
        calcularSubtotal();
        calcularTotalIva();
        calcularTotal();
        calcularSaldoPendiente();
    }

    /**
     * Determina el tipo de factura (A, B o C) según las condiciones fiscales.
     * Implementa la lógica de AFIP para determinar el tipo correcto.
     * 
     * @param condicionEmisor Condición de IVA del emisor
     * @param condicionCliente Condición de IVA del cliente
     * @return Tipo de factura a emitir
     */
    public static TipoFactura determinarTipoFactura(
            TipoCondicionIVA condicionEmisor,
            TipoCondicionIVA condicionCliente) {
        
        // Si el emisor es Responsable Inscripto
        if (condicionEmisor == TipoCondicionIVA.RESPONSABLE_INSCRIPTO) {
            // A cliente Responsable Inscripto → Factura A
            if (condicionCliente == TipoCondicionIVA.RESPONSABLE_INSCRIPTO) {
                return TipoFactura.A;
            }
            // A Consumidor Final, Monotributista o Exento → Factura B
            else {
                return TipoFactura.B;
            }
        }
        // Si el emisor es Monotributista o Exento → Siempre Factura C
        else {
            return TipoFactura.C;
        }
    }

    /**
     * Valida que las fechas de la factura sean coherentes.
     * La fecha de vencimiento debe ser posterior a la fecha de emisión.
     * 
     * @throws IllegalArgumentException si las fechas son inválidas
     */
    public void validarFechas() {
        if (this.fechaVencimiento == null || this.fechaEmision == null) {
            throw new IllegalArgumentException("Las fechas de emisión y vencimiento son obligatorias");
        }
        
        if (this.fechaVencimiento.isBefore(this.fechaEmision) || this.fechaVencimiento.isEqual(this.fechaEmision)) {
            throw new IllegalArgumentException(
                "La fecha de vencimiento debe ser posterior a la fecha de emisión. " +
                "Emisión: " + this.fechaEmision + ", Vencimiento: " + this.fechaVencimiento
            );
        }
    }

    /**
     * Valida que el cliente tenga una cuenta activa.
     * Este método debe ser llamado antes de emitir una factura.
     * 
     * @throws IllegalStateException si la cuenta no está activa
     */
    public void validarClienteActivo() {
        if (this.cliente.getEstado() != EstadoCuenta.ACTIVA) {
            throw new IllegalStateException(
                "No se puede emitir factura. El cliente no tiene cuenta activa. Estado actual: " 
                + this.cliente.getEstado().getDescripcion()
            );
        }
    }

    /**
     * Actualiza el estado de la factura a VENCIDA si cumple las condiciones:
     * - La fecha actual es posterior a la fecha de vencimiento
     * - El estado actual es PENDIENTE o PAGADA_PARCIALMENTE
     * - Tiene saldo pendiente mayor a cero
     * 
     * @return true si se actualizó el estado, false si no fue necesario
     */
    public boolean actualizarSiEstaVencida() {
        // Solo actualizar si está en estados que pueden vencer
        if (this.estado != EstadoFactura.PENDIENTE && this.estado != EstadoFactura.PAGADA_PARCIALMENTE) {
            return false;
        }
        
        // Verificar si la fecha de vencimiento ya pasó
        LocalDate hoy = LocalDate.now();
        if (hoy.isAfter(this.fechaVencimiento) && this.saldoPendiente.compareTo(BigDecimal.ZERO) > 0) {
            this.estado = EstadoFactura.VENCIDA;
            return true;
        }
        
        return false;
    }

    /**
     * Registra un pago en esta factura.
     * Actualiza automáticamente el saldo pendiente y el estado según corresponda.
     * 
     * @param pago El pago a registrar
     * @param montoAplicado El monto del pago que se aplica a esta factura
     * @return El DetallePago creado
     * @throws IllegalStateException si la factura está anulada o ya pagada totalmente
     */
    public DetallePago registrarPago(Pago pago, BigDecimal montoAplicado) {
        // Validar que puede recibir el pago
        validarPuedeRecibirPago(montoAplicado);
        
        // Crear el detalle de pago (ya valida que monto no exceda saldo pendiente)
        DetallePago detalle = DetallePago.crear(pago, this, montoAplicado);
        agregarDetallePago(detalle);
        
        // Actualizar saldo pendiente
        this.saldoPendiente = this.saldoPendiente.subtract(montoAplicado);
        
        // Actualizar estado automáticamente según el saldo
        actualizarEstadoSegunSaldo();
        
        return detalle;
    }
    
    /**
     * Valida que la factura puede recibir un pago.
     * Método de lógica de negocio que encapsula las reglas.
     * 
     * @param monto El monto del pago a validar
     * @throws IllegalStateException si la factura no puede recibir pagos
     * @throws IllegalArgumentException si el monto es inválido
     */
    public void validarPuedeRecibirPago(BigDecimal monto) {
        if (this.estado == EstadoFactura.PAGADA_TOTALMENTE) {
            throw new IllegalStateException("La factura ya está totalmente pagada");
        }
        if (this.estado == EstadoFactura.ANULADA) {
            throw new IllegalStateException("No se puede registrar pago en una factura anulada");
        }
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del pago debe ser mayor a cero");
        }
        // NOTA: Ya no validamos que el monto no exceda el saldo pendiente
        // El excedente se maneja en el Service creándolo como saldo a favor
    }
    
    /**
     * Agrega un detalle de pago manteniendo la coherencia bidireccional.
     * Método package-private para uso interno.
     */
    void agregarDetallePago(DetallePago detalle) {
        if (!this.detallesPago.contains(detalle)) {
            this.detallesPago.add(detalle);
        }
    }
    
    /**
     * Setter público para loteFacturacion.
     * Usado por LoteFacturacion para mantener bidireccionalidad.
     */
    public void setLoteFacturacion(LoteFacturacion lote) {
        this.loteFacturacion = lote;
    }
    
    /**
     * Actualiza el estado de la factura según el saldo pendiente.
     * Método privado de lógica interna.
     */
    private void actualizarEstadoSegunSaldo() {
        if (this.saldoPendiente.compareTo(BigDecimal.ZERO) == 0) {
            this.estado = EstadoFactura.PAGADA_TOTALMENTE;
        } else if (this.saldoPendiente.compareTo(this.total) < 0) {
            this.estado = EstadoFactura.PAGADA_PARCIALMENTE;
        }
        // Si el saldo es igual al total, mantiene el estado actual (PENDIENTE o VENCIDA)
    }

    /**
     * Valida si la factura puede ser anulada.
     * Solo se pueden anular facturas no pagadas o con saldo completo (sin pagos parciales).
     * 
     * @return true si la factura puede ser anulada
     */
    public boolean puedeSerAnulada() {
        // No se puede anular una factura ya anulada
        if (this.estado == EstadoFactura.ANULADA) {
            return false;
        }
        
        // No se puede anular si tiene pagos parciales
        if (this.estado == EstadoFactura.PAGADA_PARCIALMENTE) {
            return false;
        }
        
        // No se puede anular si está completamente pagada
        if (this.estado == EstadoFactura.PAGADA_TOTALMENTE) {
            return false;
        }
        
        // Se puede anular si está PENDIENTE o VENCIDA (sin pagos)
        return this.estado == EstadoFactura.PENDIENTE || this.estado == EstadoFactura.VENCIDA;
    }

    /**
     * Anula la factura cambiando su estado a ANULADA.
     * Este método debe ser llamado después de validar con puedeSerAnulada()
     * y de crear la nota de crédito correspondiente.
     * 
     * @throws IllegalStateException si la factura no puede ser anulada
     */
    public void anular() {
        if (!puedeSerAnulada()) {
            throw new IllegalStateException(
                "No se puede anular la factura. Estado actual: " + this.estado.getDescripcion() +
                ". Solo se pueden anular facturas sin pagos o con saldo completo."
            );
        }
        
        // Cambiar estado a ANULADA
        this.estado = EstadoFactura.ANULADA;
        
        // El saldo pendiente se mantiene para registro histórico
        // La nota de crédito se genera en el servicio
    }

    /**
     * Agrega una nota de crédito a la factura.
     * 
     * @param notaCredito La nota de crédito a agregar
     */
    public void agregarNotaCredito(NotaCredito notaCredito) {
        this.notasCredito.add(notaCredito);
        notaCredito.setFactura(this);
    }

    /**
     * Obtiene el período formateado como String legible.
     * Formato: "Mes Año" (ej: "Noviembre 2025")
     * 
     * @return Período formateado o null si no hay período
     */
    public String getPeriodoFormateado() {
        if (this.periodo == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.of("es", "ES"));
        String formateado = this.periodo.format(formatter);
        // Capitalizar primera letra
        return formateado.substring(0, 1).toUpperCase() + formateado.substring(1);
    }

    /**
     * Establece el período asegurando que el día sea siempre 1.
     * 
     * @param periodo Fecha del período (se usará el primer día del mes)
     */
    public void setPeriodo(LocalDate periodo) {
        if (periodo != null) {
            this.periodo = periodo.withDayOfMonth(1);
        } else {
            this.periodo = null;
        }
    }
}
