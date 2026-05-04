package com.GestionInscripcionCursos.repositorios;

import com.GestionInscripcionCursos.entidades.Mensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MensajeRepositorio extends JpaRepository<Mensaje, String> {

    @Query("SELECT m FROM Mensaje m WHERE (m.emisor.id = :usuario1 AND m.receptor.id = :usuario2) OR (m.emisor.id = :usuario2 AND m.receptor.id = :usuario1) ORDER BY m.fechaEnvio ASC")
    List<Mensaje> obtenerHistorialChat(@Param("usuario1") String usuario1, @Param("usuario2") String usuario2);
    // Cuenta los mensajes no leídos agrupados por quién te los envió
    @Query("SELECT m.emisor.id, COUNT(m) FROM Mensaje m WHERE m.receptor.id = :idReceptor AND m.leido = false GROUP BY m.emisor.id")
    List<Object[]> contarNoLeidosPorEmisor(@Param("idReceptor") String idReceptor);

    // Marca los mensajes como leídos cuando abres la conversación
    @Modifying
    @Query("UPDATE Mensaje m SET m.leido = true WHERE m.emisor.id = :idEmisor AND m.receptor.id = :idReceptor AND m.leido = false")
    void marcarComoLeidos(@Param("idEmisor") String idEmisor, @Param("idReceptor") String idReceptor);
}