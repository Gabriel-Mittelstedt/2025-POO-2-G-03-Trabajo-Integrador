package com.unam.integrador.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.unam.integrador.model.enums.EstadoCuenta;
import com.unam.integrador.model.enums.TipoCondicionIVA;

import jakarta.persistence.CascadeType;
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
public class CuentaCliente {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long IDCliente;
    
    @Column(nullable = false)
    private String nombre; 
    
    private String razonSocial; 
    
    @Column(unique = true, nullable = false)
    private String CUIT_DNI; 
    
    private String domicilio; 
    private String email; 
    private String telefono; 
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoCondicionIVA condicionIVA; 
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCuenta estado; 
    
    @Column(precision = 10, scale = 2)
    private BigDecimal saldo;
    
    // --- Relaciones ---
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServicioContratado> serviciosContratados = new ArrayList<>();
    
}
