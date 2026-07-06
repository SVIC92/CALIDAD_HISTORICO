package com.GestionInscripcionCursos.repositorios;

import com.GestionInscripcionCursos.entidades.Rubrica;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RubricaRepositorio extends JpaRepository<Rubrica, String> {

    List<Rubrica> findByCursoIdOrderByFechaGeneracionDesc(String cursoId);
}
