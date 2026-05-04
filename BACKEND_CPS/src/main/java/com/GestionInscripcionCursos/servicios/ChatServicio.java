package com.GestionInscripcionCursos.servicios;

import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.stream.Collectors;
import com.GestionInscripcionCursos.entidades.Mensaje;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.enumeraciones.Rol;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.repositorios.MensajeRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatServicio {

    @Autowired
    private MensajeRepositorio mensajeRepositorio;

    public Mensaje guardarMensaje(Usuario emisor, Usuario receptor, String contenido) throws MyException {
        validarPermisoChat(emisor, receptor);

        Mensaje mensaje = new Mensaje();
        mensaje.setEmisor(emisor);
        mensaje.setReceptor(receptor);
        mensaje.setContenido(contenido);
        return mensajeRepositorio.save(mensaje);
    }

    public List<Mensaje> obtenerHistorial(String idUsuario1, String idUsuario2) {
        return mensajeRepositorio.obtenerHistorialChat(idUsuario1, idUsuario2);
    }

    private void validarPermisoChat(Usuario emisor, Usuario receptor) throws MyException {
        Rol rolEmisor = emisor.getRol();
        Rol rolReceptor = receptor.getRol();

        boolean permisoValido = false;

        // 1. Profesor con ADMIN
        if ((rolEmisor == Rol.PROFESOR && rolReceptor == Rol.ADMIN) || 
            (rolEmisor == Rol.ADMIN && rolReceptor == Rol.PROFESOR)) {
            permisoValido = true;
        }
        // 2. Alumno con Profesor
        else if ((rolEmisor == Rol.ALUMNO && rolReceptor == Rol.PROFESOR) || 
                 (rolEmisor == Rol.PROFESOR && rolReceptor == Rol.ALUMNO)) {
            permisoValido = true;
        }
        // 3. Alumno con Alumno
        else if (rolEmisor == Rol.ALUMNO && rolReceptor == Rol.ALUMNO) {
            permisoValido = true;
        }
        // 4. Admin con Admin (Opcional, pero lógico)
        else if (rolEmisor == Rol.ADMIN && rolReceptor == Rol.ADMIN) {
             permisoValido = true;
        }

        if (!permisoValido) {
            throw new MyException("No tienes permiso para chatear con este usuario según las políticas institucionales.");
        }
    }
    // Añade este método a tu ChatServicio existente
    public Mensaje guardarMensajeArchivo(Usuario emisor, Usuario receptor, String urlArchivo, String tipoArchivo, String nombreArchivoOriginal) throws MyException {
        validarPermisoChat(emisor, receptor);

        Mensaje mensaje = new Mensaje();
        mensaje.setEmisor(emisor);
        mensaje.setReceptor(receptor);
        // Puedes usar el contenido para guardar el nombre original del archivo
        mensaje.setContenido("Archivo adjunto: " + nombreArchivoOriginal); 
        mensaje.setTipo(tipoArchivo);
        mensaje.setUrlArchivo(urlArchivo);
        
        return mensajeRepositorio.save(mensaje);
    }
    public Map<String, Long> obtenerConteoNoLeidos(String idReceptor) {
        return mensajeRepositorio.contarNoLeidosPorEmisor(idReceptor).stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0], 
                        row -> (Long) row[1]
                ));
    }

    @Transactional
    public void marcarMensajesLeidos(String idEmisor, String idReceptor) {
        mensajeRepositorio.marcarComoLeidos(idEmisor, idReceptor);
    }
}