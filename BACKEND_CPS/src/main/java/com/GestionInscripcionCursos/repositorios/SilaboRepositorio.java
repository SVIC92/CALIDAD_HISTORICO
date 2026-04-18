package com.GestionInscripcionCursos.repositorios;

import com.GestionInscripcionCursos.entidades.Silabo;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SilaboRepositorio extends JpaRepository<Silabo, String> {

    Optional<Silabo> findByCursoId(String cursoId);
}
