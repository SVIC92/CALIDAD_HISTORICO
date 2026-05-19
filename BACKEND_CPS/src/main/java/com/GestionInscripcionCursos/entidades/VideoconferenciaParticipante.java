package com.GestionInscripcionCursos.entidades;

import com.GestionInscripcionCursos.enumeraciones.RolSala;
import jakarta.persistence.*;
import lombok.Data;
@Data
@Entity
@Table(name = "videoconferencia_participantes")
public class VideoconferenciaParticipante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "videoconferencia_id")
    private Videoconferencia videoconferencia;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    private RolSala rolSala;

    private boolean invitado;
    private boolean dentroDeSala;
}