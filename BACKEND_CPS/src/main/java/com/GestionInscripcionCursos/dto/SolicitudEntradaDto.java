package com.GestionInscripcionCursos.dto;

import lombok.Data;

@Data
public class SolicitudEntradaDto {
    private String salaUuid;
    private String usuarioId;
    private String nombreUsuario;
    private boolean aprobado;
    private String rolAsignado;
}