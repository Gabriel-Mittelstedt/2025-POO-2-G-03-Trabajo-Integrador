package com.unam.integrador.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.unam.integrador.model.Servicio;

/**
 * Repositorio para gestionar operaciones de persistencia de Servicio.
 */
@Repository
public interface ServicioRepository extends JpaRepository<Servicio, Long> {
    
    /**
     * Busca un servicio por su nombre (único).
     * @param nombre Nombre del servicio
     * @return Optional con el servicio si existe
     */
    Optional<Servicio> findByNombre(String nombre);
    
    /**
     * Busca todos los servicios activos.
     * @return Lista de servicios activos
     */
    List<Servicio> findByActivoTrue();
    
    /**
     * Busca servicios por nombre o descripción (búsqueda parcial, case-insensitive).
     * @param nombre Término a buscar en el nombre
     * @param descripcion Término a buscar en la descripción
     * @return Lista de servicios que coinciden
     */
    List<Servicio> findByNombreContainingIgnoreCaseOrDescripcionContainingIgnoreCase(
        String nombre, String descripcion);
}
