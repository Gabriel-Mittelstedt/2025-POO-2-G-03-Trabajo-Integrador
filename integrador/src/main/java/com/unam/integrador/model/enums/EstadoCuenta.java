package com.unam.integrador.model.enums;

/**
 * Enumeración que representa los posibles estados de una cuenta de cliente.
 * 
 * <p>Define los estados operativos que puede tener un cliente en el sistema,
 * controlando su capacidad para realizar operaciones comerciales.</p>
 */
public enum EstadoCuenta {
    
    /**
     * Cliente activo y operativo.
     * <p>Puede contratar servicios y realizar pagos.
     * Este es el estado por defecto al crear una cuenta.</p>
     */
    ACTIVA("Activa"),
    
    /**
     * Cliente suspendido temporalmente.
     * <p>No puede contratar nuevos servicios pero mantiene los existentes.
     * Típicamente usado por mora en pagos o problemas administrativos temporales.</p>
     */
    SUSPENDIDA("Suspendida"),
    
    /**
     * Cliente dado de baja del sistema.
     * <p>No puede realizar ninguna operación.</p>
     */
    BAJA("Baja");
    
    /**
     * Descripción legible del estado para mostrar en la interfaz de usuario.
     */
    private final String descripcion;
    

    EstadoCuenta(String descripcion) {
        this.descripcion = descripcion;
    }
    

    public String getDescripcion() {
        return descripcion;
    }
}
