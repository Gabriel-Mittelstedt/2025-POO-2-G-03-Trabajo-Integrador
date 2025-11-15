package com.unam.integrador.model.enums;

/**
 * Enumeración que representa las alícuotas de IVA aplicables en Argentina.
 * 
 * <p>Define los porcentajes de IVA vigentes según la normativa de AFIP
 * para diferentes tipos de bienes y servicios.</p>
 */
public enum TipoAlicuotaIVA {
    
    /**
     * Alícuota general del 21%.
     * <p>Aplicable a la mayoría de bienes y servicios.
     * Es la alícuota estándar más utilizada.</p>
     */
    IVA_21,
    
    /**
     * Alícuota reducida del 10.5%.
     * <p>Aplicable a productos de primera necesidad como:
     * alimentos básicos, medicamentos, libros, periódicos.</p>
     */
    IVA_10_5,
    
    /**
     * Alícuota incrementada del 27%.
     * <p>Aplicable a servicios específicos como:
     * gas, electricidad, agua, telecomunicaciones.</p>
     */
    IVA_27,
    
    /**
     * Alícuota mínima del 2.5%.
     * <p>Aplicable a operaciones especiales como:
     * venta de animales vivos, bienes de capital, cereales.</p>
     */
    IVA_2_5,
    
    /**
     * Operación exenta de IVA.
     * <p>No se aplica IVA. Utilizado para actividades exentas
     * por ley (ej: servicios educativos, médicos).</p>
     */
    EXENTO
}