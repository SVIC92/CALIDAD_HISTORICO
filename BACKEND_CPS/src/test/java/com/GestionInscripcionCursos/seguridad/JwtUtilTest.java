package com.GestionInscripcionCursos.seguridad;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de {@link JwtUtil}.
 *
 * <p>Se construye siempre con el secreto configurado en blanco ("") para
 * forzar el uso del {@code FALLBACK_JWT_SECRET} interno, de modo que la
 * generacion/validacion de tokens sea determinista y no dependa de
 * variables de entorno externas (JWT_SECRET).
 */
@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    private static final long EXPIRACION_NORMAL_MS = 60_000L;

    private JwtUtil jwtUtil;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil("", EXPIRACION_NORMAL_MS);
        userDetails = User.withUsername("alumno@dominio.com")
                .password("secreto")
                .authorities("ROLE_ALUMNO")
                .build();
    }

    // =====================================================================
    // generateToken
    // =====================================================================
    @Nested
    @DisplayName("generateToken")
    class GenerateToken {

        @Test
        @DisplayName("genera un token JWT no nulo con formato de 3 segmentos")
        void generaTokenConFormatoValido() {
            String token = jwtUtil.generateToken(userDetails);

            assertNotNull(token);
            assertEquals(3, token.split("\\.").length);
        }

        @Test
        @DisplayName("el token generado contiene el username como subject")
        void tokenContieneUsernameComoSubject() {
            String token = jwtUtil.generateToken(userDetails);

            assertEquals("alumno@dominio.com", jwtUtil.extractUsername(token));
        }

        @Test
        @DisplayName("el token generado contiene el claim rol con la primera authority")
        void tokenContieneClaimRol() {
            String token = jwtUtil.generateToken(userDetails);

            String rol = jwtUtil.extractClaim(token, claims -> claims.get("rol", String.class));

            assertEquals("ROLE_ALUMNO", rol);
        }

        @Test
        @DisplayName("el token generado tiene una fecha de expiracion posterior a la actual")
        void tokenTieneExpiracionFutura() {
            String token = jwtUtil.generateToken(userDetails);

            Date expiracion = jwtUtil.extractExpiration(token);

            assertTrue(expiracion.after(new Date()));
        }
    }

    // =====================================================================
    // validateToken
    // =====================================================================
    @Nested
    @DisplayName("validateToken")
    class ValidateToken {

        @Test
        @DisplayName("con token valido y mismo usuario retorna true")
        void tokenValidoParaMismoUsuarioRetornaTrue() {
            String token = jwtUtil.generateToken(userDetails);

            assertTrue(jwtUtil.validateToken(token, userDetails));
        }

        @Test
        @DisplayName("con token valido pero username distinto retorna false")
        void tokenValidoConUsernameDistintoRetornaFalse() {
            String token = jwtUtil.generateToken(userDetails);
            UserDetails otroUsuario = User.withUsername("otro@dominio.com")
                    .password("x")
                    .authorities("ROLE_ALUMNO")
                    .build();

            assertFalse(jwtUtil.validateToken(token, otroUsuario));
        }

        @Test
        @DisplayName("con token expirado retorna false")
        void tokenExpiradoRetornaFalse() {
            JwtUtil jwtUtilExpirado = new JwtUtil("", -10_000L);
            String tokenExpirado = jwtUtilExpirado.generateToken(userDetails);

            assertFalse(jwtUtilExpirado.validateToken(tokenExpirado, userDetails));
        }

        @Test
        @DisplayName("con token manipulado/invalido retorna false")
        void tokenManipuladoRetornaFalse() {
            String token = jwtUtil.generateToken(userDetails);
            String tokenManipulado = token.substring(0, token.length() - 2) + "xx";

            assertFalse(jwtUtil.validateToken(tokenManipulado, userDetails));
        }

        @Test
        @DisplayName("con token con formato invalido retorna false")
        void tokenConFormatoInvalidoRetornaFalse() {
            assertFalse(jwtUtil.validateToken("no-es-un-jwt", userDetails));
        }
    }

    // =====================================================================
    // extractClaim / extractUsername / extractExpiration
    // =====================================================================
    @Nested
    @DisplayName("extraccion de claims")
    class ExtraccionClaims {

        @Test
        @DisplayName("extractUsername retorna el subject del token")
        void extractUsernameRetornaSubject() {
            String token = jwtUtil.generateToken(userDetails);

            assertEquals(userDetails.getUsername(), jwtUtil.extractUsername(token));
        }

        @Test
        @DisplayName("extractClaim permite extraer el subject via funcion personalizada")
        void extractClaimConFuncionPersonalizada() {
            String token = jwtUtil.generateToken(userDetails);

            String subject = jwtUtil.extractClaim(token, Claims::getSubject);

            assertEquals("alumno@dominio.com", subject);
        }
    }
}
