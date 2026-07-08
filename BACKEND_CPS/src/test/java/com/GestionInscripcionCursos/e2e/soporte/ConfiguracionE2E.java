package com.GestionInscripcionCursos.e2e.soporte;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Clase base para las pruebas Selenium E2E.
 *
 * Requiere que el backend (mvn spring-boot:run, :8080) y el frontend
 * (npm start, :5173) estén corriendo localmente antes de ejecutar.
 *
 * Ejecutar con: mvn verify -Pe2e
 * Parámetros opcionales: -De2e.baseUrl=http://localhost:5173 -De2e.headless=true
 *
 * Si un test falla, queda una captura de pantalla en target/e2e-screenshots/.
 */
@ExtendWith(CapturaPantallaAlFallar.class)
public abstract class ConfiguracionE2E {

    /**
     * 30s: el backend habla con Postgres en la nube (Neon, con cold-start) y varias
     * operaciones (guardar edición, eliminar, registrar inscripción) llegaron a tardar
     * más de 20s en pruebas reales, dejando la UI en el estado correcto justo después
     * de que el wait ya había expirado.
     */
    protected static final Duration ESPERA_EXPLICITA = Duration.ofSeconds(30);

    /**
     * Evidencia paso a paso para el informe (no para depuración de fallos, eso ya lo
     * cubre CapturaPantallaAlFallar): desactivada por defecto para no ensuciar
     * target/e2e-screenshots/ en cada corrida normal. Activar con -De2e.evidencia=true.
     */
    private static final boolean CAPTURAR_EVIDENCIA = Boolean.parseBoolean(System.getProperty("e2e.evidencia", "false"));
    private final AtomicInteger contadorEvidencia = new AtomicInteger(1);

    protected WebDriver driver;
    protected WebDriverWait espera;
    protected String baseUrl;

    @BeforeEach
    void configurarDriver() {
        WebDriverManager.chromedriver().setup();

        baseUrl = System.getProperty("e2e.baseUrl", "http://localhost:5173");
        boolean headless = Boolean.parseBoolean(System.getProperty("e2e.headless", "false"));

        ChromeOptions opciones = new ChromeOptions();
        if (headless) {
            opciones.addArguments("--headless=new");
        }
        // Alto a propósito: los diálogos de curso/horarios tienen más campos de los que
        // caben en una ventana de 900px, y el scroll interno del Dialog complica ubicar
        // elementos de forma confiable.
        opciones.addArguments("--window-size=1440,2400");
        opciones.addArguments("--remote-allow-origins=*");

        driver = new ChromeDriver(opciones);
        espera = new WebDriverWait(driver, ESPERA_EXPLICITA);
    }

    @AfterEach
    void cerrarDriver() {
        if (driver != null) {
            driver.quit();
        }
    }

    /**
     * Lee una propiedad de sistema requerida (cuenta de prueba, etc.) y, si falta,
     * OMITE el test en vez de fallarlo: no tener la cuenta configurada es un
     * problema de entorno local, no un defecto del código bajo prueba.
     */
    protected static String propiedadRequerida(String clave) {
        String valor = System.getProperty(clave);
        Assumptions.assumeTrue(valor != null && !valor.isBlank(),
                () -> "Falta la propiedad -D" + clave + "=... (ver docs/E2E_SELENIUM.md). Test omitido.");
        return valor;
    }

    /** Guarda una captura numerada en target/e2e-screenshots/ si -De2e.evidencia=true. */
    protected void capturar(String nombrePaso) {
        if (!CAPTURAR_EVIDENCIA || driver == null) {
            return;
        }
        try {
            byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Path directorio = Path.of("target", "e2e-screenshots");
            Files.createDirectories(directorio);
            String nombreArchivo = String.format("%s_%02d_%s.png",
                    getClass().getSimpleName(), contadorEvidencia.getAndIncrement(), nombrePaso);
            Files.write(directorio.resolve(nombreArchivo), png);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
