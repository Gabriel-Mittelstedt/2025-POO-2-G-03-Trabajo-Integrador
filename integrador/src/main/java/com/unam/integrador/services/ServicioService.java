package com.unam.integrador.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unam.integrador.model.Servicio;
import com.unam.integrador.model.ServicioContratado;
import com.unam.integrador.repositories.ServicioRepository;

/**
 * Servicio de aplicación para gestionar la lógica de negocio de Servicios.
 */
@Service
public class ServicioService {
    
    @Autowired
    private ServicioRepository servicioRepository;
    
    /**
     * Crea un nuevo servicio.
     * @param servicio el servicio a crear (debe tener nombre, precio y alícuota IVA)
     * @return el servicio guardado
     * @throws IllegalArgumentException si el nombre ya existe
     */
    @Transactional
    public Servicio crearServicio(Servicio servicio) {
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
    
    /**
     * HU-18: Modifica un servicio existente validando sus datos.
     * Si el precio cambia, actualiza también los contratos activos.
     * Las facturas ya emitidas NO se modifican.
     * @param id ID del servicio a modificar
     * @param servicioActualizado Servicio con los nuevos datos
     * @return el servicio modificado
     * @throws IllegalArgumentException si no existe o los datos son inválidos
     */
    @Transactional
    public Servicio modificarServicio(Long id, Servicio servicioActualizado) {
        Servicio servicio = buscarPorId(id);
        
        // Verificar que el nombre no esté en uso por otro servicio
        if (!servicio.getNombre().equals(servicioActualizado.getNombre())) {
            servicioRepository.findByNombre(servicioActualizado.getNombre())
                .ifPresent(s -> {
                    if (!s.getIDServicio().equals(id)) {
                        throw new IllegalArgumentException(
                            "Ya existe otro servicio con el nombre: " + servicioActualizado.getNombre());
                    }
                });
        }
        
        // Modificar servicio
        servicio.modificar(
            servicioActualizado.getNombre(),
            servicioActualizado.getDescripcion(),
            servicioActualizado.getPrecio(),
            servicioActualizado.getAlicuotaIVA()
        );
        
        // Actualizar contratos activos si el precio cambió
        for (ServicioContratado contrato : servicio.getContratos()) {
            if (contrato.getActivo()) {
                contrato.setPrecioContratado(servicio.getPrecio());
            }
        }
        
        // Persistir cambios
        return servicioRepository.save(servicio);
    }
    
    /**
     * HU-19: Da de baja (desactiva) un servicio.
     * El servicio desactivado no puede ser asignado a nuevos clientes,
     * pero se mantiene en los contratos existentes.
     * @param id ID del servicio a dar de baja
     * @return el servicio desactivado
     * @throws IllegalArgumentException si no se encuentra el servicio
     */
    @Transactional
    public Servicio darDeBajaServicio(Long id) {
        Servicio servicio = buscarPorId(id);
        servicio.desactivar();
        return servicioRepository.save(servicio);
    }
    
    /**
     * Reactiva un servicio previamente desactivado.
     * @param id ID del servicio a reactivar
     * @return el servicio reactivado
     * @throws IllegalArgumentException si no se encuentra el servicio
     */
    @Transactional
    public Servicio reactivarServicio(Long id) {
        Servicio servicio = buscarPorId(id);
        servicio.activar();
        return servicioRepository.save(servicio);
    }
}
