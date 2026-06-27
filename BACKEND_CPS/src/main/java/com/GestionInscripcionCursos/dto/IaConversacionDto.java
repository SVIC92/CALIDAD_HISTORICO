package com.GestionInscripcionCursos.dto;

import java.util.List;

public record IaConversacionDto(List<IaMensajeDto> mensajes, int totalMensajes) {
}
