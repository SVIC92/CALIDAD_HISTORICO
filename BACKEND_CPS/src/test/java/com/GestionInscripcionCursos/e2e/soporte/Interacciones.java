package com.GestionInscripcionCursos.e2e.soporte;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.LocalTime;
import java.util.Random;

/**
 * Los inputs nativos type="date"/"time" representan su valor internamente
 * como yyyy-MM-dd / HH:mm (spec HTML) sin importar el locale del sistema,
 * pero sendKeys() escribe sobre la representación visual, que SÍ depende
 * del locale (ej. formato 12h con AM/PM en inglés). Para evitar tests
 * frágiles por el locale del equipo donde corren, se fija el valor por JS
 * y se disparan los eventos que React necesita para detectar el cambio.
 */
public final class Interacciones {

    private Interacciones() {
    }

    /**
     * WebElement.clear() no es confiable contra estos TextField controlados por React:
     * en varios casos (p. ej. el buscador al escribir una segunda búsqueda, o el campo
     * "Nombre" al editar, que llega prellenado) el DOM se vacía pero el estado de React
     * no se entera, así que un re-render posterior restaura el valor viejo y lo que
     * escribe sendKeys() después queda CONCATENADO al texto anterior en vez de
     * reemplazarlo. fijarValor() reemplaza el valor completo por JS (funciona con
     * &lt;input&gt; y &lt;textarea&gt;) y dispara los eventos que React sí escucha.
     */
    public static void fijarValor(WebDriver driver, WebElement campo, String valor) {
        ((JavascriptExecutor) driver).executeScript(
                "const el = arguments[0];" +
                        "const valor = arguments[1];" +
                        "const prototipo = el.tagName === 'TEXTAREA' " +
                        "    ? window.HTMLTextAreaElement.prototype : window.HTMLInputElement.prototype;" +
                        "const setter = Object.getOwnPropertyDescriptor(prototipo, 'value').set;" +
                        "setter.call(el, valor);" +
                        "el.dispatchEvent(new Event('input', { bubbles: true }));" +
                        "el.dispatchEvent(new Event('change', { bubbles: true }));",
                campo, valor);
    }

    private static final String LETRAS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final Random ALEATORIO = new Random();

    /**
     * El campo "Nombre" de Curso solo acepta letras/espacios/apóstrofes/guiones
     * (ver nombreCursoRegex en CursosListado.jsx), por lo que el sufijo único
     * para no chocar entre ejecuciones de la suite debe ser alfabético, no un
     * timestamp numérico.
     */
    public static String nombreUnico(String base) {
        StringBuilder sufijo = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sufijo.append(LETRAS.charAt(ALEATORIO.nextInt(LETRAS.length())));
        }
        return base + " " + sufijo;
    }

    private static final String ALFANUMERICO = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    /**
     * Si no se envía "codigoCurso", el backend lo autogenera truncando el nombre
     * (normalizarCodigoCurso en CursoServicio.java) — como todos los nombres de
     * esta suite comparten el mismo prefijo, ese código autogenerado siempre
     * coincide y choca con cursos huérfanos de corridas anteriores fallidas
     * ("Ya existe un curso con el codigo ..."). Por eso se envía uno explícito
     * y único por curso.
     */
    public static String codigoUnico(String prefijo) {
        StringBuilder sufijo = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sufijo.append(ALFANUMERICO.charAt(ALEATORIO.nextInt(ALFANUMERICO.length())));
        }
        return prefijo + "-" + sufijo;
    }

    /**
     * La cuenta ALUMNO de pruebas no se "desinscribe" entre corridas (el flujo de
     * autoinscripción no expone cancelar inscripción en la UI) y además ya tiene
     * matriculados cursos reales del entorno (p. ej. "Redes", lunes por la noche),
     * así que un horario fijo — o incluso uno aleatorio de solo la hora en punto,
     * con pocos valores posibles (0-4) — termina chocando con algo ya inscrito de
     * una corrida anterior tras repetir la suite unas pocas veces. Se sortea hora
     * de madrugada (0-5h) MÁS minuto (0-59), dando 360 puntos de inicio posibles
     * en vez de 5, para que las colisiones entre corridas sean prácticamente nulas.
     */
    public static LocalTime inicioAleatorioDeMadrugada() {
        return LocalTime.of(ALEATORIO.nextInt(6), ALEATORIO.nextInt(60)); // 00:00–05:59
    }

    public static final String[] DIAS = {
            "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"
    };

    /**
     * Sortear también el día (no solo hora+minuto) multiplica por 7 el espacio de
     * horarios posibles — útil cuando se corre la suite muchas veces seguidas (p. ej.
     * para juntar evidencia de un informe) y 360 combinaciones de una sola madrugada
     * dejan de ser suficientes para evitar colisiones con corridas previas.
     *
     * Devuelve la POSICIÓN (1-7, no el texto): CursosPage.seleccionarDia elige la
     * opción del combo MUI por posición en vez de por texto exacto, para no depender
     * de que "Miércoles"/"Sábado" viajen intactos entre el literal Java y el DOM.
     */
    public static int diaAleatorioPosicion() {
        return 1 + ALEATORIO.nextInt(DIAS.length); // 1..7
    }
}
