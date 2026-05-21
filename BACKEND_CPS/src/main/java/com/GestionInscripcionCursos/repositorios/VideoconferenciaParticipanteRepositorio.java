package com.GestionInscripcionCursos.repositorios;

import com.GestionInscripcionCursos.entidades.VideoconferenciaParticipante;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface VideoconferenciaParticipanteRepositorio extends JpaRepository<VideoconferenciaParticipante, Long> {
    List<VideoconferenciaParticipante> findByVideoconferenciaSalaUuid(String salaUuid);
    Optional<VideoconferenciaParticipante> findByVideoconferenciaSalaUuidAndUsuarioId(String salaUuid, String usuarioId);
    long countByVideoconferenciaIdAndDentroDeSalaTrue(Long videoconferenciaId);
}