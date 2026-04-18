package com.GestionInscripcionCursos.repositorios;

import com.GestionInscripcionCursos.entidades.Carrera;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CarreraRepositorio extends JpaRepository<Carrera, String> {

    Optional<Carrera> findByCodigoIgnoreCase(String codigo);

    Optional<Carrera> findByNombreIgnoreCase(String nombre);
}
