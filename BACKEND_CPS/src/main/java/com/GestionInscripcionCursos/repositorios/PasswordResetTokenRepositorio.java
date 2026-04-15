package com.GestionInscripcionCursos.repositorios;

import com.GestionInscripcionCursos.entidades.PasswordResetToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PasswordResetTokenRepositorio extends JpaRepository<PasswordResetToken, String> {

    Optional<PasswordResetToken> findByToken(String token);

    void deleteByUsuarioId(String usuarioId);
}
