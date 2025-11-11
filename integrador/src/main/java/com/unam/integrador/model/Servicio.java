package com.unam.integrador.model;

import java.math.BigDecimal;
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
import lombok.Data;


@Data
@Entity
public class Servicio {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long IDServicio;
    
    @Column(nullable = false, unique = true)
    private String nombre; 
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio; 

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoAlicuotaIVA alicuotaIVA; 
    
    @Column(nullable = false)
    private boolean activo = true; 
    
    // --- Relaciones ---
    
    //Un servicio puede estar en muchos contratos
    @OneToMany(mappedBy = "servicio")
    private List<ServicioContratado> contratos = new ArrayList<>();
}