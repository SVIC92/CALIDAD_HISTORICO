package com.GestionInscripcionCursos.repositorios;

import com.GestionInscripcionCursos.entidades.IaHistorial;
import com.GestionInscripcionCursos.entidades.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IaHistorialRepositorio extends JpaRepository<IaHistorial, String> {

    Optional<IaHistorial> findByUsuario(Usuario usuario);
}
