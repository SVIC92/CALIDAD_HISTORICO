package com.GestionInscripcionCursos;

import com.GestionInscripcionCursos.seguridad.JwtFiltro;
import com.GestionInscripcionCursos.servicios.UsuarioServicio;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SeguridadWeb {

    @Autowired
    private UsuarioServicio usuarioServicio;

    @Autowired
    private JwtFiltro jwtFiltro; // Inyectamos nuestro nuevo filtro

    @Value("${app.cors.allowed-origin-patterns}")
    private String corsAllowedOriginPatterns;

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(usuarioServicio).passwordEncoder(new BCryptPasswordEncoder());
    }

    // Exponemos el AuthenticationManager para poder usarlo en AuthControlador
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class).build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors().and()
            .csrf().disable()
            .authorizeRequests()
                // Endpoints públicos (sin token)
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/api/auth/forgot-password").permitAll()
                .requestMatchers("/api/auth/reset-password").permitAll()
                .requestMatchers("/api/portal/registro").permitAll()
                .requestMatchers("/api/portal/registrar").permitAll()
                .requestMatchers("/api/portal/login").permitAll()
                .requestMatchers("/api/portal/").permitAll()
                .requestMatchers("/api/carrera/lista").permitAll()   // necesario en la pantalla de Registro
                .requestMatchers("/api/subtitulos/interno").permitAll()  // webhook del microservicio de transcripción
                .requestMatchers("/ws-chat/**").permitAll()          // WebSocket STOMP (auth via JWT en CONNECT)
                // Todo lo demás requiere autenticación
                .requestMatchers("/api/**").authenticated()
            .and()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        http.addFilterBefore(jwtFiltro, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> originPatterns = Arrays.stream(corsAllowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());

        configuration.setAllowedOriginPatterns(originPatterns);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}