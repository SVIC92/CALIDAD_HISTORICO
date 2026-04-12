package com.GestionInscripcionCursos.repositorios;

import com.GestionInscripcionCursos.entidades.Reporte;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReporteRepositorio extends JpaRepository<Reporte, String> {

    @Query("SELECT i FROM Inscripcion i WHERE i.usuario.id = :idUser AND i.curso.id = :idCurso")
    public Reporte buscarInscripcionPorIdUserIdCurso(@Param("idUser") String idUser, @Param("idCurso") String idCurso);
    
    @Query("SELECT r FROM Reporte r JOIN r.usuario u JOIN r.actividad a WHERE u.id = :idUser AND a.id = :idActividad")
    public Reporte buscarReportePorIdUserIdActividad(@Param("idUser") String idUser, @Param("idActividad") String idActividad);
    
    
    @Query("SELECT r FROM Reporte r JOIN r.actividad a WHERE a.id = :id")
    public List<Reporte> buscarReportesPorIdActividad(@Param("id") String id);
    
    @Query("SELECT r FROM Reporte r WHERE r.id = :id")
    public Reporte buscarPorId(@Param("id") String id);
}
