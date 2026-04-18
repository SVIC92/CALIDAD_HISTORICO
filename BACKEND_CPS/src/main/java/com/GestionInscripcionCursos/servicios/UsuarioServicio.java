package com.GestionInscripcionCursos.servicios;

import com.GestionInscripcionCursos.dto.ProfesorResumenDto;
import com.GestionInscripcionCursos.dto.UsuarioResumenDto;
import com.GestionInscripcionCursos.entidades.Carrera;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.enumeraciones.Rol;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.repositorios.CarreraRepositorio;
import com.GestionInscripcionCursos.repositorios.UsuarioRepositorio;

import jakarta.transaction.Transactional;
import java.util.ArrayList;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioServicio implements UserDetailsService {

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Autowired
    private CarreraRepositorio carreraRepositorio;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public void registrar(String nombre, String email, String password, String password2) throws MyException {
        registrar(nombre, email, password, password2, null, null);
    }

    @Transactional
    public void registrar(String nombre, String email, String password, String password2, String carreraReferencia, Integer cicloActual) throws MyException {

        nombre = nombre == null ? null : nombre.trim();
        email = email == null ? null : email.trim().toLowerCase();
        password = password == null ? null : password.trim();
        password2 = password2 == null ? null : password2.trim();

        validar(nombre, email, password, password2);

        Usuario usuario = new Usuario();

        Carrera carrera = resolverCarreraSiExiste(carreraReferencia);

        usuario.setNombre(nombre);

        usuario.setEmail(email);

        usuario.setPassword(passwordEncoder.encode(password));

        usuario.setFechaCreacion(new Date());

        usuario.setRol(Rol.ALUMNO);

        usuario.setCarrera(carrera);
        usuario.setActivo(true);

        usuario.setTwoFactorEnabled(false);

        if (cicloActual != null) {
            if (cicloActual <= 0 || cicloActual > 14) {
                throw new MyException("El ciclo actual debe estar entre 1 y 14");
            }
            usuario.setCicloActual(cicloActual);
        } else {
            usuario.setCicloActual(1);
        }

        usuarioRepositorio.save(usuario);
    }

    private Carrera resolverCarreraSiExiste(String carreraReferencia) {
        if (carreraReferencia == null || carreraReferencia.isBlank()) {
            return null;
        }

        return carreraRepositorio.findById(carreraReferencia)
                .orElseGet(() -> carreraRepositorio.findByNombreIgnoreCase(carreraReferencia)
                .orElse(null));
    }

    @Transactional
    public void crearOActualizarAdminPrueba(String nombre, String email, String password) throws MyException {
        if (nombre == null || nombre.isBlank()) {
            throw new MyException("El nombre no puede ser nulo o estar vacio");
        }
        if (email == null || email.isBlank()) {
            throw new MyException("El email no puede ser nulo o estar vacio");
        }
        if (password == null || password.length() <= 5) {
            throw new MyException("La contrasena debe tener mas de 5 digitos");
        }

        Usuario usuario = usuarioRepositorio.buscarPorEmail(email);
        if (usuario == null) {
            usuario = new Usuario();
            usuario.setFechaCreacion(new Date());
            usuario.setEmail(email);
        }

        usuario.setNombre(nombre);
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setRol(Rol.ADMIN);
        usuario.setActivo(true);
        usuarioRepositorio.save(usuario);
    }

    private void validar(String nombre, String email, String password, String password2) throws MyException {

        if (nombre == null || nombre.isBlank()) {
            throw new MyException("El nombre no puede ser nulo o estar vacío");
        }

        if (email == null || email.isBlank()) {
            throw new MyException("El email no puede ser nulo o estar vacío");
        }
        
        String regex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";

        if (!email.matches(regex)) {
            throw new MyException("El email no tiene un formato válido");
        }
        
        Usuario usuario = usuarioRepositorio.buscarPorEmail(email);
        
        if (usuario != null) {
            throw new MyException("El email está en uso");
        }

        if (password == null || password.isBlank() || password.length() <= 5) {
            throw new MyException("La contraseña no puede estar vacía, y debe tener más de 5 dígitos");
        }

        if (password2 == null || password2.isBlank()) {
            throw new MyException("Debes confirmar la contraseña");
        }

        if (!password.equals(password2)) {
            throw new MyException("Las contraseñas ingresadas deben ser iguales");
        }
        if (email.length() > 255 || password.length() > 255 || password2.length() > 255) {
            throw new MyException("El email o las contraseñas no debe pasar de 255 caracteres");
        }
    }

    public Usuario getOne(String id) {
        return usuarioRepositorio.getOne(id);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        Usuario usuario = usuarioRepositorio.buscarPorEmail(email);

        if (usuario != null) {
            if (!usuario.isActivo()) {
                throw new UsernameNotFoundException("Usuario inactivo");
            }

            List<GrantedAuthority> permisos = new ArrayList<>();
            GrantedAuthority p = new SimpleGrantedAuthority("ROLE_" + usuario.getRol().toString());
            permisos.add(p);

            // ¡Se eliminó el bloque de HttpSession porque las API REST no usan sesiones web!

            return new User(usuario.getEmail(), usuario.getPassword(), permisos);
            
        } else {
            // Spring Security requiere que lancemos esta excepción en lugar de devolver null
            throw new UsernameNotFoundException("Usuario no encontrado con el email: " + email);
        }
    }
    
    public Usuario buscarEmail(String id) {
        return usuarioRepositorio.buscarPorEmail(id);
    }

    @Transactional
    public Usuario guardar(Usuario usuario) {
        return usuarioRepositorio.save(usuario);
    }

    public List<ProfesorResumenDto> listarProfesores() {
        return usuarioRepositorio.buscarPorRol(Rol.PROFESOR)
                .stream()
                .map(u -> new ProfesorResumenDto(u.getId(), u.getNombre(), u.getEmail()))
                .toList();
    }

    public List<UsuarioResumenDto> listarUsuarios() {
        return usuarioRepositorio.findAll()
                .stream()
                .sorted((a, b) -> a.getNombre().compareToIgnoreCase(b.getNombre()))
                .map(u -> new UsuarioResumenDto(u.getId(), u.getNombre(), u.getEmail(), u.getRol().name()))
                .toList();
    }

    public Usuario buscarPorId(String id) throws MyException {
        if (id == null || id.isBlank()) {
            throw new MyException("El id del usuario es obligatorio");
        }
        return usuarioRepositorio.findById(id)
                .orElseThrow(() -> new MyException("Usuario no encontrado"));
    }

    @Transactional
    public Usuario crearUsuarioAdmin(
            String nombre,
            String email,
            String password,
            String rolTexto,
            String carreraReferencia,
            Integer cicloActual
    ) throws MyException {
        nombre = nombre == null ? null : nombre.trim();
        email = email == null ? null : email.trim().toLowerCase();
        password = password == null ? null : password.trim();

        if (nombre == null || nombre.isBlank()) {
            throw new MyException("El nombre no puede ser nulo o estar vacío");
        }
        if (email == null || email.isBlank()) {
            throw new MyException("El email no puede ser nulo o estar vacío");
        }
        if (usuarioRepositorio.buscarPorEmail(email) != null) {
            throw new MyException("El email está en uso");
        }
        if (password == null || password.length() <= 5) {
            throw new MyException("La contraseña debe tener más de 5 caracteres");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setEmail(email);
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setFechaCreacion(new Date());
        usuario.setRol(resolverRol(rolTexto));
        usuario.setCarrera(resolverCarreraSiExiste(carreraReferencia));
        usuario.setActivo(true);
        usuario.setTwoFactorEnabled(false);

        if (cicloActual != null) {
            if (cicloActual <= 0 || cicloActual > 14) {
                throw new MyException("El ciclo actual debe estar entre 1 y 14");
            }
            usuario.setCicloActual(cicloActual);
        } else {
            usuario.setCicloActual(1);
        }

        return usuarioRepositorio.save(usuario);
    }

    @Transactional
    public Usuario actualizarUsuarioAdmin(
            String id,
            String nombre,
            String email,
            String password,
            String rolTexto,
            String carreraReferencia,
            Integer cicloActual
    ) throws MyException {
        Usuario usuario = buscarPorId(id);

        if (nombre != null) {
            String nombreNormalizado = nombre.trim();
            if (nombreNormalizado.isBlank()) {
                throw new MyException("El nombre no puede estar vacío");
            }
            usuario.setNombre(nombreNormalizado);
        }

        if (email != null) {
            String emailNormalizado = email.trim().toLowerCase();
            if (emailNormalizado.isBlank()) {
                throw new MyException("El email no puede estar vacío");
            }
            Usuario existente = usuarioRepositorio.buscarPorEmail(emailNormalizado);
            if (existente != null && !existente.getId().equals(usuario.getId())) {
                throw new MyException("El email está en uso");
            }
            usuario.setEmail(emailNormalizado);
        }

        if (password != null) {
            String passwordNormalizado = password.trim();
            if (!passwordNormalizado.isEmpty()) {
                if (passwordNormalizado.length() <= 5) {
                    throw new MyException("La contraseña debe tener más de 5 caracteres");
                }
                usuario.setPassword(passwordEncoder.encode(passwordNormalizado));
            }
        }

        if (rolTexto != null) {
            usuario.setRol(resolverRol(rolTexto));
        }

        if (carreraReferencia != null) {
            String carreraNormalizada = carreraReferencia.trim();
            if (carreraNormalizada.isBlank()) {
                usuario.setCarrera(null);
            } else {
                usuario.setCarrera(resolverCarreraSiExiste(carreraNormalizada));
            }
        }

        if (cicloActual != null) {
            if (cicloActual <= 0 || cicloActual > 14) {
                throw new MyException("El ciclo actual debe estar entre 1 y 14");
            }
            usuario.setCicloActual(cicloActual);
        }

        return usuarioRepositorio.save(usuario);
    }

    @Transactional
    public Usuario desactivarUsuario(String id) throws MyException {
        Usuario usuario = buscarPorId(id);
        usuario.setActivo(false);
        return usuarioRepositorio.save(usuario);
    }

    @Transactional
    public Usuario activarUsuario(String id) throws MyException {
        Usuario usuario = buscarPorId(id);
        usuario.setActivo(true);
        return usuarioRepositorio.save(usuario);
    }

    private Rol resolverRol(String rolTexto) throws MyException {
        if (rolTexto == null || rolTexto.isBlank()) {
            return Rol.ALUMNO;
        }
        try {
            return Rol.valueOf(rolTexto.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new MyException("Rol inválido. Valores permitidos: ADMIN, PROFESOR, ALUMNO");
        }
    }

    

}
