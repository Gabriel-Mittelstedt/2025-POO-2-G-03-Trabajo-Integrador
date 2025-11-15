package com.unam.integrador.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unam.integrador.model.Servicio;
import com.unam.integrador.repository.ServicioRepository;

/**
 * Servicio de aplicación para gestionar la lógica de negocio de Servicios.
 */
@Service
public class ServicioService {
    
    @Autowired
    private ServicioRepository servicioRepository;
    
    /**
     * Crea un nuevo servicio validando sus datos.
     * @param servicio el servicio a crear (debe tener nombre, precio y alícuota IVA)
     * @return el servicio guardado
     * @throws IllegalArgumentException si los datos son inválidos o el nombre ya existe
     */
    @Transactional
    public Servicio crearServicio(Servicio servicio) {
        // Validar datos del servicio
        servicio.validar();
        
        // Verificar que no exista otro servicio con el mismo nombre
        if (servicioRepository.findByNombre(servicio.getNombre()).isPresent()) {
            throw new IllegalArgumentException(
                "Ya existe un servicio con el nombre: " + servicio.getNombre());
        }
        
        // Guardar servicio
        return servicioRepository.save(servicio);
    }
    
    /**
     * Lista todos los servicios registrados en el sistema.
     * @return Lista de todos los servicios
     */
    public List<Servicio> listarTodos() {
        return servicioRepository.findAll();
    }
    
    /**
     * Lista solo los servicios activos.
     * @return Lista de servicios activos
     */
    public List<Servicio> listarActivos() {
        return servicioRepository.findByActivoTrue();
    }
    
    /**
     * Busca un servicio por ID.
     * @param id ID del servicio
     * @return el servicio encontrado
     * @throws IllegalArgumentException si no se encuentra el servicio
     */
    public Servicio buscarPorId(Long id) {
        return servicioRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Servicio no encontrado con ID: " + id));
    }
    
    /**
     * Busca servicios por nombre o descripción.
     * @param termino Término de búsqueda
     * @return la lista de servicios que coinciden
     */
    public List<Servicio> buscar(String termino) {
        return servicioRepository
            .findByNombreContainingIgnoreCaseOrDescripcionContainingIgnoreCase(
                termino, termino);
    }
}
