package com.GestionInscripcionCursos.servicios;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de {@link TwoFactorServicio}: generacion de secreto TOTP
 * (RFC 6238), construccion de la URL otpauth y validacion de codigos.
 * El uso de HmacSHA1 aqui esta revisado y aceptado en SonarQube (Security
 * Hotspot java:S4790) por ser el algoritmo exigido por el estandar TOTP.
 */
class TwoFactorServicioTest {

    private TwoFactorServicio twoFactorServicio;

    @BeforeEach
    void setUp() {
        twoFactorServicio = new TwoFactorServicio();
        ReflectionTestUtils.setField(twoFactorServicio, "issuer", "GCIPlus");
    }

    @Nested
    @DisplayName("generarSecreto")
    class GenerarSecreto {

        @Test
        @DisplayName("genera un secreto Base32 no vacio sin caracteres de relleno")
        void generaSecretoBase32Valido() {
            String secreto = twoFactorServicio.generarSecreto();

            assertNotNull(secreto);
            assertFalse(secreto.isBlank());
            assertFalse(secreto.contains("="));
            assertTrue(secreto.matches("[A-Z2-7]+"));
        }

        @Test
        @DisplayName("genera secretos distintos en cada llamada")
        void generaSecretosDistintos() {
            String s1 = twoFactorServicio.generarSecreto();
            String s2 = twoFactorServicio.generarSecreto();

            assertNotEquals(s1, s2);
        }
    }

    @Nested
    @DisplayName("construirOtpAuthUrl")
    class ConstruirOtpAuthUrl {

        @Test
        @DisplayName("incluye el issuer, el email y los parametros del estandar TOTP")
        void construyeUrlCorrectamente() {
            String url = twoFactorServicio.construirOtpAuthUrl("ana@dominio.com", "SECRETOBASE32");

            assertTrue(url.startsWith("otpauth://totp/"));
            assertTrue(url.contains("secret=SECRETOBASE32"));
            assertTrue(url.contains("issuer=GCIPlus"));
            assertTrue(url.contains("algorithm=SHA1"));
            assertTrue(url.contains("digits=6"));
            assertTrue(url.contains("period=30"));
        }

        @Test
        @DisplayName("codifica caracteres especiales del email en la URL")
        void codificaCaracteresEspeciales() {
            String url = twoFactorServicio.construirOtpAuthUrl("ana+test@dominio.com", "SECRETO");

            assertFalse(url.contains("ana+test@dominio.com"));
        }
    }

    @Nested
    @DisplayName("validarCodigo")
    class ValidarCodigo {

        @Test
        @DisplayName("con secreto nulo retorna false")
        void secretoNuloRetornaFalse() {
            assertFalse(twoFactorServicio.validarCodigo(null, "123456"));
        }

        @Test
        @DisplayName("con codigo que no son 6 digitos retorna false")
        void codigoConFormatoInvalidoRetornaFalse() {
            String secreto = twoFactorServicio.generarSecreto();
            assertFalse(twoFactorServicio.validarCodigo(secreto, "12AB56"));
            assertFalse(twoFactorServicio.validarCodigo(secreto, "12345"));
            assertFalse(twoFactorServicio.validarCodigo(secreto, null));
        }

        @Test
        @DisplayName("con codigo incorrecto pero bien formado retorna false")
        void codigoIncorrectoRetornaFalse() {
            String secreto = twoFactorServicio.generarSecreto();
            assertFalse(twoFactorServicio.validarCodigo(secreto, "000000"));
        }

        @Test
        @DisplayName("con el codigo TOTP generado para el instante actual retorna true")
        void codigoActualEsValido() {
            String secreto = "JBSWY3DPEHPK3PXP";
            long timeIndex = System.currentTimeMillis() / 1000L / 30L;
            String codigoEsperado = (String) ReflectionTestUtils.invokeMethod(
                    twoFactorServicio, "generarCodigoTotp", secreto, timeIndex);

            assertTrue(twoFactorServicio.validarCodigo(secreto, codigoEsperado));
        }
    }

    @Nested
    @DisplayName("generarQrPng")
    class GenerarQrPng {

        @Test
        @DisplayName("genera bytes PNG no vacios para una URL otpauth valida")
        void generaPngValido() {
            String url = twoFactorServicio.construirOtpAuthUrl("ana@dominio.com", "SECRETO");
            byte[] png = twoFactorServicio.generarQrPng(url, 200);

            assertTrue(png.length > 0);
            // Firma PNG: 0x89 'P' 'N' 'G'
            assertEquals((byte) 0x89, png[0]);
            assertEquals('P', png[1]);
            assertEquals('N', png[2]);
            assertEquals('G', png[3]);
        }
    }
}
