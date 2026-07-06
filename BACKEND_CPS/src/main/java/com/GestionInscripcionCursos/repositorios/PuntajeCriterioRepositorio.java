package com.GestionInscripcionCursos.repositorios;

import com.GestionInscripcionCursos.entidades.PuntajeCriterio;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PuntajeCriterioRepositorio extends JpaRepository<PuntajeCriterio, String> {

    List<PuntajeCriterio> findByReporteId(String reporteId);

    void deleteByReporteId(String reporteId);
}
