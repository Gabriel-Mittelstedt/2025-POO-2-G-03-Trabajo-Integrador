package com.unam.integrador.model.enums;

import java.math.BigDecimal;

/**
 * Enumeración de las alícuotas de IVA.
 * <p>Define los diferentes porcentajes de IVA que pueden aplicarse 
 * a los servicios según la normativa fiscal vigente.</p>
 * <p><b>Alícuotas disponibles:</b></p>
 * <ul>
 *   <li><b>IVA 21%:</b> Alícuota general (mayoría de servicios)</li>
 *   <li><b>IVA 10.5%:</b> Alícuota reducida (servicios específicos)</li>
 *   <li><b>IVA 27%:</b> Alícuota incrementada (servicios de lujo)</li>
 *   <li><b>IVA 2.5%:</b> Alícuota mínima (casos especiales)</li>
 *   <li><b>EXENTO:</b> Sin IVA (servicios exentos por ley)</li>
 * </ul>
 */ 
public enum TipoAlicuotaIVA {
    IVA_21(new BigDecimal("21"), "IVA 21%"),
    IVA_10_5(new BigDecimal("10.5"), "IVA 10.5%"),
    IVA_27(new BigDecimal("27"), "IVA 27%"),
    IVA_2_5(new BigDecimal("2.5"), "IVA 2.5%"),
    EXENTO(BigDecimal.ZERO, "Exento");
    
    private final BigDecimal porcentaje;
    private final String descripcion;
    
    TipoAlicuotaIVA(BigDecimal porcentaje, String descripcion) {
        this.porcentaje = porcentaje;
        this.descripcion = descripcion;
    }
    
    /**
     * Obtiene el porcentaje numérico de la alícuota.
     * @return Porcentaje de IVA como BigDecimal
     */
    public BigDecimal getPorcentaje() {
        return porcentaje;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
}