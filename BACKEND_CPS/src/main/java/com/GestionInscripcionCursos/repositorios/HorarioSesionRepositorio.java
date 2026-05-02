package com.GestionInscripcionCursos.repositorios;

import com.GestionInscripcionCursos.entidades.HorarioSesion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HorarioSesionRepositorio extends JpaRepository<HorarioSesion, String> {

    List<HorarioSesion> findByCursoIdOrderByDiaSemanaAscHoraInicioAsc(String cursoId);
    @Query("SELECT h FROM HorarioSesion h WHERE LOWER(h.aula) = LOWER(:aula) AND LOWER(h.diaSemana) = LOWER(:diaSemana)")
    List<HorarioSesion> buscarPorAulaYDia(@Param("aula") String aula, @Param("diaSemana") String diaSemana);
    @Query("SELECT h FROM HorarioSesion h WHERE h.curso.profesorAsignado.id = :profesorId AND h.curso.estado = 'ACTIVO' ORDER BY h.diaSemana ASC, h.horaInicio ASC")
    List<HorarioSesion> buscarHorariosPorProfesor(@Param("profesorId") String profesorId);
    @Query("SELECT h FROM HorarioSesion h JOIN h.curso c JOIN c.inscripciones i WHERE i.usuario.id = :alumnoId AND i.estado = 'APROBADO' AND c.estado = 'ACTIVO' ORDER BY h.diaSemana ASC, h.horaInicio ASC")
    List<HorarioSesion> buscarHorariosPorAlumno(@Param("alumnoId") String alumnoId);
}
