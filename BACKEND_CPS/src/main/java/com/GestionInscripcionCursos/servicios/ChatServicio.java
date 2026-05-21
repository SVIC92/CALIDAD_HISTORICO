package com.GestionInscripcionCursos.servicios;

import org.springframework.transaction.annotation.Transactional;
import com.GestionInscripcionCursos.dto.GifResultadoDto;
import java.util.Map;
import java.util.stream.Collectors;
import com.GestionInscripcionCursos.entidades.Mensaje;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.enumeraciones.Rol;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.repositorios.MensajeRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChatServicio {
    private static final int GIF_LIMIT = 12;
    private static final Pattern MEDIA_TENOR_PATTERN = Pattern.compile("https://media\\.tenor\\.com/[^\"\\s>]+", Pattern.CASE_INSENSITIVE);

    @Autowired
    private MensajeRepositorio mensajeRepositorio;

    private final HttpClient httpClient = HttpClient.newHttpClient();

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

    public List<GifResultadoDto> buscarGifs(String query) throws MyException {
        try {
            String termino = query == null ? "" : query.trim();
            String endpoint = construirUrlTenor(termino.isBlank() ? "reactions" : termino);

            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .GET()
                    .header("Accept", "application/json")
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return List.of();
            }

            List<GifResultadoDto> resultados = new ArrayList<>();
            Set<String> urlsUnicas = new LinkedHashSet<>();

            Matcher matcher = MEDIA_TENOR_PATTERN.matcher(response.body());
            while (matcher.find() && resultados.size() < GIF_LIMIT) {
                String mediaUrl = matcher.group();
                if (!esUrlGifCompatible(mediaUrl) || !urlsUnicas.add(mediaUrl)) {
                    continue;
                }

                resultados.add(new GifResultadoDto(
                        String.valueOf(resultados.size()),
                        construirTituloDesdeUrl(mediaUrl),
                        mediaUrl,
                        mediaUrl
                ));
            }

            return resultados;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    private String construirUrlTenor(String query) {
        String terminoNormalizado = query.toLowerCase().trim().replaceAll("[^a-z0-9áéíóúüñ\\s-]", "").replaceAll("\\s+", "-");
        if (terminoNormalizado.isBlank()) {
            terminoNormalizado = "reactions";
        }
        return "https://tenor.com/search/" + terminoNormalizado + "-gifs";
    }

    private boolean esUrlGifCompatible(String url) {
        String urlLimpia = url.toLowerCase();
        return urlLimpia.endsWith(".gif") || urlLimpia.endsWith(".webp");
    }

    private String construirTituloDesdeUrl(String url) {
        try {
            String path = URI.create(url).getPath();
            if (path == null || path.isBlank()) {
                return "GIF";
            }

            String nombre = path.substring(path.lastIndexOf('/') + 1);
            nombre = nombre.replaceAll("\\.(gif|webp)$", "");
            nombre = nombre.replace('-', ' ').replace('_', ' ').trim();
            return nombre.isBlank() ? "GIF" : nombre;
        } catch (Exception ignored) {
            return "GIF";
        }
    }
}