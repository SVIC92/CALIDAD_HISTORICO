package com.GestionInscripcionCursos.entidades;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
@Data
@Entity
@Table(name = "videoconferencias")
public class Videoconferencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;

    @Column(unique = true, nullable = false)
    private String salaUuid;

    private LocalDateTime fechaInicio;
    private int capacidadMaxima;
    private boolean esPublica;

    @Column(name = "modo_estudio_flexible")
    private boolean modoEstudioFlexible = false;
    
    @ManyToOne
    @JoinColumn(name = "creador_id")
    private Usuario creador;
}