package com.unam.integrador.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Value Object que representa un período de facturación.
 * Encapsula la lógica para calcular días efectivos y determinar si es un período parcial.
 */
public class PeriodoFacturacion {
    
    private final LocalDate inicio;
    private final LocalDate fin;
    private final int diasEfectivos;
    private final int diasDelMes;
    private final boolean esParcial;
    
    /**
     * Constructor que calcula automáticamente los días efectivos y determina si es parcial.
     * 
     * @param inicio Fecha de inicio del período
     * @param fin Fecha de fin del período
     */
    public PeriodoFacturacion(LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null) {
            throw new IllegalArgumentException("Las fechas de inicio y fin son obligatorias");
        }
        
        if (fin.isBefore(inicio)) {
            throw new IllegalArgumentException("La fecha de fin debe ser posterior o igual a la fecha de inicio");
        }
        
        this.inicio = inicio;
        this.fin = fin;
        
        // Calcular días efectivos (inclusivo)
        this.diasEfectivos = (int) ChronoUnit.DAYS.between(inicio, fin) + 1;
        
        // Días del mes (tomamos el mes de la fecha de fin)
        this.diasDelMes = fin.lengthOfMonth();
        
        // Es parcial si no cubre todo el mes
        this.esParcial = diasEfectivos < diasDelMes;
    }
    
    /**
     * Retorna true si el período no cubre el mes completo.
     */
    public boolean esParcial() {
        return esParcial;
    }
    
    /**
     * Obtiene los días efectivos del período.
     */
    public int getDiasEfectivos() {
        return diasEfectivos;
    }
    
    /**
     * Obtiene los días totales del mes.
     */
    public int getDiasDelMes() {
        return diasDelMes;
    }
    
    /**
     * Obtiene la fecha de inicio del período.
     */
    public LocalDate getInicio() {
        return inicio;
    }
    
    /**
     * Obtiene la fecha de fin del período.
     */
    public LocalDate getFin() {
        return fin;
    }
    
    /**
     * Genera una descripción del período para mostrar en la factura.
     * Ejemplo: "15 al 31 de Noviembre 2025"
     */
    public String generarDescripcionPeriodo() {
        return String.format("%d al %d de %s %d",
            inicio.getDayOfMonth(),
            fin.getDayOfMonth(),
            obtenerNombreMes(fin.getMonthValue()),
            fin.getYear()
        );
    }
    
    /**
     * Obtiene el nombre del mes en español.
     */
    private String obtenerNombreMes(int mes) {
        String[] meses = {
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        };
        return meses[mes - 1];
    }
    
    @Override
    public String toString() {
        return String.format("PeriodoFacturacion[%s a %s, %d/%d días%s]",
            inicio, fin, diasEfectivos, diasDelMes, esParcial ? " (parcial)" : "");
    }
}
