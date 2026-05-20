package com.GestionInscripcionCursos.dto;

import lombok.Data;

@Data
public class CambioRolDinamicoDto {
    private String salaUuid;
    private String usuarioId;
    private String nuevoRol; 
}