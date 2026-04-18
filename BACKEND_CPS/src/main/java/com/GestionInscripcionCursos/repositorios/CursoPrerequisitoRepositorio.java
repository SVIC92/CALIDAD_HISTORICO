package com.GestionInscripcionCursos.repositorios;

import com.GestionInscripcionCursos.entidades.CursoPrerequisito;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CursoPrerequisitoRepositorio extends JpaRepository<CursoPrerequisito, String> {

    List<CursoPrerequisito> findByCursoId(String cursoId);
}
