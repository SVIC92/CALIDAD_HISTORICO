package com.GestionInscripcionCursos.repositorios;

import com.GestionInscripcionCursos.entidades.CriterioRubrica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CriterioRubricaRepositorio extends JpaRepository<CriterioRubrica, String> {
}
