package com.GestionInscripcionCursos.servicios;

import com.GestionInscripcionCursos.dto.UsuarioConectadoDto;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class PresenciaUsuarioServicio {

    private final Map<String, EstadoConexion> sesiones = new ConcurrentHashMap<>();
    private final List<SseEmitter> emisores = new CopyOnWriteArrayList<>();
    private final Duration timeoutConexion;

    public PresenciaUsuarioServicio(@Value("${app.presencia.timeout-segundos:90}") long timeoutSegundos) {
        this.timeoutConexion = Duration.ofSeconds(timeoutSegundos);
    }

    public void registrarActividad(String email, Collection<? extends GrantedAuthority> authorities) {
        Instant ahora = Instant.now();
        String rol = extraerRol(authorities);

        sesiones.compute(email, (k, actual) -> {
            if (actual == null) {
                return new EstadoConexion(email, rol, ahora, ahora);
            }
            return new EstadoConexion(
                    email,
                    rol != null ? rol : actual.rol(),
                    actual.conectadoDesde(),
                    ahora
            );
        });

        notificarCambios();
    }

    public List<UsuarioConectadoDto> listarConectados() {
        limpiarInactivos();

        List<UsuarioConectadoDto> conectados = new ArrayList<>();
        sesiones.values().stream()
                .sorted(Comparator.comparing(EstadoConexion::email, String.CASE_INSENSITIVE_ORDER))
                .forEach(s -> conectados.add(new UsuarioConectadoDto(
                        s.email(),
                        s.rol(),
                        s.conectadoDesde(),
                        s.ultimaActividad()
                )));
        return conectados;
    }

    public void registrarEmisor(SseEmitter emisor) {
        emisores.add(emisor);
        emisor.onCompletion(() -> emisores.remove(emisor));
        emisor.onTimeout(() -> emisores.remove(emisor));
        emisor.onError(ex -> emisores.remove(emisor));
        enviarSnapshot(emisor);
    }

    private void notificarCambios() {
        List<UsuarioConectadoDto> snapshot = listarConectados();
        for (SseEmitter emisor : emisores) {
            try {
                emisor.send(SseEmitter.event()
                        .name("usuarios-conectados")
                        .data(snapshot));
            } catch (IOException e) {
                emisor.complete();
                emisores.remove(emisor);
            }
        }
    }

    private void enviarSnapshot(SseEmitter emisor) {
        try {
            emisor.send(SseEmitter.event()
                    .name("usuarios-conectados")
                    .data(listarConectados()));
        } catch (IOException e) {
            emisor.complete();
            emisores.remove(emisor);
        }
    }

    private void limpiarInactivos() {
        Instant limite = Instant.now().minus(timeoutConexion);
        sesiones.entrySet().removeIf(entry -> entry.getValue().ultimaActividad().isBefore(limite));
    }

    private String extraerRol(Collection<? extends GrantedAuthority> authorities) {
        if (authorities == null || authorities.isEmpty()) {
            return null;
        }

        String authority = authorities.iterator().next().getAuthority();
        if (authority == null) {
            return null;
        }

        return authority.startsWith("ROLE_") ? authority.substring(5) : authority;
    }

    private record EstadoConexion(
            String email,
            String rol,
            Instant conectadoDesde,
            Instant ultimaActividad
    ) {
    }
}
