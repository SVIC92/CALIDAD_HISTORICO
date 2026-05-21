package com.GestionInscripcionCursos.dto;

import lombok.Data;

@Data
public class VideoconferenciaParticipanteDto {
    private Long id;
    private String usuarioId;
    private String nombreUsuario;
    private String email;
    private String rolSala;
    private boolean invitado;
    private boolean dentroDeSala;
}