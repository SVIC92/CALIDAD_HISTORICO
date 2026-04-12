package com.GestionInscripcionCursos.repositorios;

import com.GestionInscripcionCursos.entidades.Curso;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CursoRepositorio extends JpaRepository<Curso, String> {

    @Query("SELECT c FROM Curso c")
    public List<Curso> buscarCursos();

    @Query("SELECT c FROM Curso c WHERE c.id = :id")
    public Curso buscarPorId(@Param("id") String id);

    @Query("SELECT c FROM Curso c LEFT JOIN c.inscripciones i LEFT JOIN i.usuario u WHERE (i.usuario.id != :idUser OR i.estado != 'APROBADO' OR i.usuario.id IS NULL) AND (u.rol = 'PROFESOR' OR u.id IS NULL)")
    public List<Curso> buscarCursosDisponiblesProfesor(@Param("idUser") String idUser);

    @Query("SELECT c FROM Curso c JOIN c.inscripciones i JOIN i.usuario u WHERE i.usuario.id = :idUser AND i.estado = 'APROBADO' AND u.rol = 'PROFESOR'")
    public List<Curso> buscarCursosInscritosProfesor(@Param("idUser") String idUser);

     @Query("SELECT c FROM Curso c LEFT JOIN c.inscripciones i LEFT JOIN i.usuario u WHERE (i.usuario.id != :idUser OR i.estado != 'APROBADO' OR i.usuario.id IS NULL) AND (u.rol = 'ALUMNO' OR u.id IS NULL)")
    public List<Curso> buscarCursosDisponiblesAlumno(@Param("idUser") String idUser);

    @Query("SELECT c FROM Curso c JOIN c.inscripciones i JOIN i.usuario u WHERE i.usuario.id = :idUser AND i.estado = 'APROBADO' AND u.rol = 'ALUMNO'")
    public List<Curso> buscarCursosInscritosAlumno(@Param("idUser") String idUser);

}
