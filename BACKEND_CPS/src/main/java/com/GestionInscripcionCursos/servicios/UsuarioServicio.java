package com.GestionInscripcionCursos.servicios;

import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.enumeraciones.Rol;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.repositorios.UsuarioRepositorio;
import jakarta.servlet.http.HttpSession;

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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class UsuarioServicio implements UserDetailsService {

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Transactional
    public void registrar(String nombre, String email, String password, String password2) throws MyException {

        validar(nombre, email, password, password2);

        Usuario usuario = new Usuario();

        usuario.setNombre(nombre);

        usuario.setEmail(email);

        usuario.setPassword(new BCryptPasswordEncoder().encode(password));

        usuario.setFechaCreacion(new Date());

        usuario.setRol(Rol.ALUMNO);

        usuarioRepositorio.save(usuario);
    }

    private void validar(String nombre, String email, String password, String password2) throws MyException {

        if (nombre.isEmpty() || nombre == null) {
            throw new MyException("El nombre no puede ser nulo o estar vacío");
        }

        if (email.isEmpty() || email == null) {
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

        if (password.isEmpty() || password == null || password.length() <= 5) {
            throw new MyException("La contraseña no puede estar vacía, y debe tener más de 5 dígitos");
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

            List<GrantedAuthority> permisos = new ArrayList<>();

            GrantedAuthority p = new SimpleGrantedAuthority("ROLE_" + usuario.getRol().toString());

            permisos.add(p);

            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();

            HttpSession session = attr.getRequest().getSession(true);

            session.setAttribute("usuariosession", usuario);

            return new User(usuario.getEmail(), usuario.getPassword(), permisos);
        } else {
            return null;
        }
    }
    
    public Usuario buscarEmail(String id) {
        return usuarioRepositorio.buscarPorEmail(id);
    }

    

}
