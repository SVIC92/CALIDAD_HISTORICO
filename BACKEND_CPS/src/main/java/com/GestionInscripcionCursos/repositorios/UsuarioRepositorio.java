package com.GestionInscripcionCursos.repositorios;

import com.GestionInscripcionCursos.enumeraciones.Rol;
import com.GestionInscripcionCursos.entidades.Usuario;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepositorio extends JpaRepository<Usuario,String>{
    
    @Query("SELECT u FROM Usuario u WHERE u.email = :email")
    public Usuario buscarPorEmail(@Param("email") String email);

    @Query("SELECT u FROM Usuario u WHERE u.rol = :rol ORDER BY u.nombre ASC")
    public List<Usuario> buscarPorRol(@Param("rol") Rol rol);
    
}
