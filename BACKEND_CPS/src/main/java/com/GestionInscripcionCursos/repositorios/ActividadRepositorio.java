package com.GestionInscripcionCursos.repositorios;

import com.GestionInscripcionCursos.entidades.Actividad;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ActividadRepositorio extends JpaRepository<Actividad,String>{
    
    @Query("SELECT a FROM Actividad a JOIN a.curso c WHERE c.id = :id")
    public List<Actividad> buscarActividadesPorIdCurso(@Param("id") String id);
    
    @Query("SELECT a FROM Actividad a WHERE a.id = :id")
    public Actividad buscarPorId(@Param("id") String id);

    
}
