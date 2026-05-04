package com.GestionInscripcionCursos.dto;

import jakarta.validation.constraints.NotBlank;

public record MensajeRequestDto(
        
        @NotBlank(message = "El ID del receptor es obligatorio")
        String idReceptor,
        
        @NotBlank(message = "El contenido del mensaje no puede estar vacío")
        String contenido
) {
}