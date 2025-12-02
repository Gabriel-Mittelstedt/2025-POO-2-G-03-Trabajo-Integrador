package com.unam.integrador.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.unam.integrador.model.enums.TipoAlicuotaIVA;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
public class Servicio {
    
    /**
     * Identificador único del servicio.
     * Generado automáticamente por la base de datos.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long IDServicio;
    
    /**
     * Nombre del servicio.
     * Debe ser único en el sistema.
     */
    @Column(nullable = false, unique = true, length = 100)
    @NotBlank(message = "El nombre del servicio es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre; 
    
     /**
     * Descripción detallada del servicio (opcional).
     * <p>Proporciona información adicional sobre las características 
     * o condiciones del servicio.</p>
     */
    @Column(length = 255)
    @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
    private String descripcion;
    
    /**
     * Precio base del servicio sin IVA incluido.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a cero")
    @Digits(integer = 8, fraction = 2, message = "El precio no puede superar los 99,999,999.99")
    private BigDecimal precio; 

    /**
     * Alícuota de IVA aplicable al servicio.
     * <p>Define el porcentaje de IVA que se aplicará al precio base 
     * al momento de facturar el servicio.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "La alícuota de IVA es obligatoria")
    private TipoAlicuotaIVA alicuotaIVA; 
    
    /**
     * Indica si el servicio está activo y disponible para contratar.
     */
    @Column(nullable = false)
    private boolean activo = true;
    
    /**
     * Contratos de clientes que tienen este servicio.
     */
    @OneToMany(mappedBy = "servicio")
    private List<ServicioContratado> contratos = new ArrayList<>();

    // --- Constructor ---
 
    public Servicio(String nombre, String descripcion, BigDecimal precio, TipoAlicuotaIVA alicuotaIVA) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.alicuotaIVA = alicuotaIVA;
        this.activo = true;
    }

    // --- Métodos de negocio ---
    
    /**
     * Calcula el monto de IVA según la alícuota configurada.
     * @return Monto del IVA
     */
    public BigDecimal calcularIva() {
        if (precio == null || alicuotaIVA == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal porcentaje = obtenerPorcentajeIva();
        return precio.multiply(porcentaje)
                     .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calcula el precio total incluyendo IVA.
     * @return Precio con IVA
     */
    public BigDecimal calcularPrecioConIva() {
        return precio.add(calcularIva());
    }
    
    /**
     * Calcula el precio total con IVA usando un precio específico.
     * Útil para calcular el total de un servicio contratado con precio histórico.
     * @param precioBase el precio base a usar para el cálculo
     * @return Precio con IVA aplicado
     */
    public BigDecimal calcularPrecioConIva(BigDecimal precioBase) {
        if (precioBase == null || alicuotaIVA == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal porcentaje = obtenerPorcentajeIva();
        BigDecimal iva = precioBase.multiply(porcentaje)
                                   .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        return precioBase.add(iva);
    }
    
    /**
     * Obtiene el porcentaje numérico de la alícuota de IVA.
     * @return Porcentaje de IVA
     */
    private BigDecimal obtenerPorcentajeIva() {
        switch (alicuotaIVA) {
            case IVA_21: return new BigDecimal("21");
            case IVA_10_5: return new BigDecimal("10.5");
            case IVA_27: return new BigDecimal("27");
            case IVA_2_5: return new BigDecimal("2.5");
            case EXENTO: return BigDecimal.ZERO;
            default: return BigDecimal.ZERO;
        }
    }
    
    /**
     * Activa el servicio para que pueda ser asignado a clientes.
     */
    public void activar() {
        this.activo = true;
    }
    
    /**
     * Desactiva el servicio para que no pueda ser asignado a nuevos clientes 
     * (baja lógica).
     */
    public void desactivar() {
        this.activo = false;
    }
    
    /**
     * Determina si el servicio puede ser facturado.
     * Un servicio solo puede facturarse si está activo.
     * @return true si el servicio puede facturarse, false en caso contrario
     */
    public boolean puedeFacturarse() {
        return this.activo;
    }
    
    /**
     * Modifica los datos del servicio.
     * Permite actualizar nombre, descripción, precio y alícuota IVA.
     * @param nuevoNombre Nuevo nombre del servicio
     * @param nuevaDescripcion Nueva descripción del servicio
     * @param nuevoPrecio Nuevo precio del servicio
     * @param nuevaAlicuota Nueva alícuota de IVA
     */
    public void modificar(String nuevoNombre, String nuevaDescripcion, 
                          BigDecimal nuevoPrecio, TipoAlicuotaIVA nuevaAlicuota) {
        this.nombre = nuevoNombre;
        this.descripcion = nuevaDescripcion;
        this.precio = nuevoPrecio;
        this.alicuotaIVA = nuevaAlicuota;
    }
}