package com.GestionInscripcionCursos.repositorios;

import com.GestionInscripcionCursos.entidades.Videoconferencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface VideoconferenciaRepositorio extends JpaRepository<Videoconferencia, Long> {
    Optional<Videoconferencia> findBySalaUuid(String salaUuid);
    @Query("SELECT v FROM Videoconferencia v WHERE v.esPublica = true AND " +
           "(SELECT COUNT(p) FROM VideoconferenciaParticipante p WHERE p.videoconferencia = v AND p.dentroDeSala = true) < v.capacidadMaxima")
    List<Videoconferencia> findSalasPublicasDisponibles();
}