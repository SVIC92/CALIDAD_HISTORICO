package com.GestionInscripcionCursos.repositorios;

import com.GestionInscripcionCursos.entidades.HorarioSesion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HorarioSesionRepositorio extends JpaRepository<HorarioSesion, String> {

    List<HorarioSesion> findByCursoIdOrderByDiaSemanaAscHoraInicioAsc(String cursoId);
}
