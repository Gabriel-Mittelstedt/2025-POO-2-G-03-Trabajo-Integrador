package com.unam.integrador.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.unam.integrador.model.enums.EstadoCuenta;
import com.unam.integrador.model.enums.EstadoFactura;
import com.unam.integrador.model.enums.TipoCondicionIVA;
import com.unam.integrador.model.enums.TipoFactura;

import jakarta.persistence.*;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
public class Factura {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idFactura;

    @Column(nullable = false)
    private int serie;

    @Column(nullable = false)
    private int nroFactura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private CuentaCliente cliente;

    // --- Atributos de la Factura ---
    private LocalDate fechaEmision;
    private LocalDate fechaVencimiento;
    private String periodo;

    @Enumerated(EnumType.STRING)
    private TipoFactura tipo;
    
    @Enumerated(EnumType.STRING)
    private EstadoFactura estado;

    // --- Campos Calculados y Opcionales ---
    // Se inicializan en 0 o null y se calculan con un método.
    private BigDecimal subtotal;
    private double descuento;
    private String motivoDescuento;
    private BigDecimal totalIva;
    private BigDecimal saldoPendiente;
    private BigDecimal total;

    //--Relaciones--
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id") 
    private List<ItemFactura> detalleFactura = new ArrayList<>();

    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<NotaCredito> notasCredito = new ArrayList<>();

    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Pago> pagos = new ArrayList<>();

    //CONSTRUCTOR
    public Factura(int serie, int nroFactura, CuentaCliente cliente, LocalDate fechaEmision, 
                   LocalDate fechaVencimiento, String periodo, TipoFactura tipo) {
        
        this.serie = serie;
        this.nroFactura = nroFactura;
        this.cliente = cliente;
        this.fechaEmision = fechaEmision;
        this.fechaVencimiento = fechaVencimiento;
        this.periodo = periodo;
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
     * Registra un pago total para esta factura.
     * Actualiza el saldo pendiente y el estado a PAGADA_TOTALMENTE.
     * 
     * @param pago El pago a registrar
     * @throws IllegalStateException si la factura ya está pagada o anulada
     */
    public void registrarPagoTotal(Pago pago) {
        if (this.estado == EstadoFactura.PAGADA_TOTALMENTE) {
            throw new IllegalStateException("La factura ya está totalmente pagada");
        }
        if (this.estado == EstadoFactura.ANULADA) {
            throw new IllegalStateException("No se puede registrar pago en una factura anulada");
        }
        
        // Agregar el pago a la lista
        this.pagos.add(pago);
        pago.setFactura(this);
        
        // Actualizar saldo pendiente
        this.saldoPendiente = BigDecimal.ZERO;
        
        // Cambiar estado a pagada totalmente
        this.estado = EstadoFactura.PAGADA_TOTALMENTE;
    }

    /**
     * Registra un pago parcial para esta factura.
     * Actualiza el saldo pendiente y el estado a PAGADA_PARCIALMENTE.
     * 
     * @param pago El pago parcial a registrar
     * @throws IllegalStateException si la factura está anulada o ya pagada totalmente
     * @throws IllegalArgumentException si el monto del pago excede el saldo pendiente
     */
    public void registrarPagoParcial(Pago pago) {
        if (this.estado == EstadoFactura.PAGADA_TOTALMENTE) {
            throw new IllegalStateException("La factura ya está totalmente pagada");
        }
        if (this.estado == EstadoFactura.ANULADA) {
            throw new IllegalStateException("No se puede registrar pago en una factura anulada");
        }
        if (pago.getMonto().compareTo(this.saldoPendiente) > 0) {
            throw new IllegalArgumentException("El monto del pago no puede exceder el saldo pendiente");
        }
        
        // Agregar el pago a la lista
        this.pagos.add(pago);
        pago.setFactura(this);
        
        // Actualizar saldo pendiente
        this.saldoPendiente = this.saldoPendiente.subtract(pago.getMonto());
        
        // Actualizar estado
        if (this.saldoPendiente.compareTo(BigDecimal.ZERO) == 0) {
            this.estado = EstadoFactura.PAGADA_TOTALMENTE;
        } else {
            this.estado = EstadoFactura.PAGADA_PARCIALMENTE;
        }
    }
}
