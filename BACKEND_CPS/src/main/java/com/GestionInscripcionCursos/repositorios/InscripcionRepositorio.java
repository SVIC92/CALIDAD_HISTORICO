package com.GestionInscripcionCursos.repositorios;

import com.GestionInscripcionCursos.entidades.Inscripcion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InscripcionRepositorio extends JpaRepository<Inscripcion,String>{
    
    @Query("SELECT i FROM Inscripcion i WHERE i.usuario.id = :idUser AND i.curso.id = :idCurso")
    public Inscripcion buscarInscripcionPorIdUserIdCurso(@Param("idUser") String idUser,@Param("idCurso") String idCurso);
    
    @Query("SELECT i FROM Inscripcion i INNER JOIN i.usuario u WHERE i.estado = 'PENDIENTE' AND u.rol = 'PROFESOR'")
    public List<Inscripcion> listarPendientesProfesor();
    
    @Query("SELECT i FROM Inscripcion i INNER JOIN i.usuario u WHERE i.estado != 'PENDIENTE' AND u.rol = 'PROFESOR'")
    public List<Inscripcion> listarRealizadasProfesor();
    
    @Query("SELECT i FROM Inscripcion i INNER JOIN i.usuario u WHERE i.estado = 'PENDIENTE' AND u.rol = 'ALUMNO'")
    public List<Inscripcion> listarPendientesAlumno();
    
    @Query("SELECT i FROM Inscripcion i INNER JOIN i.usuario u WHERE i.estado != 'PENDIENTE' AND u.rol = 'ALUMNO'")
    public List<Inscripcion> listarRealizadasAlumno();
    

    
    
}
