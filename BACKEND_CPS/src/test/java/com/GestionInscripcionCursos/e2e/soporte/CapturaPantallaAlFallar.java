package com.GestionInscripcionCursos.e2e.soporte;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Guarda una captura de pantalla en target/e2e-screenshots/ cuando un test E2E falla.
 *
 * Usa AfterTestExecutionCallback (no TestWatcher) a propósito: TestWatcher.testFailed
 * se invoca DESPUÉS de que corre @AfterEach, cuando ConfiguracionE2E ya cerró el
 * navegador (driver.quit()) — capturar en ese punto lanza NoSuchSessionException.
 * AfterTestExecutionCallback corre antes de @AfterEach, con el navegador aún vivo.
 */
public class CapturaPantallaAlFallar implements AfterTestExecutionCallback {

    @Override
    public void afterTestExecution(ExtensionContext context) {
        if (context.getExecutionException().isEmpty()) {
            return;
        }
        Object instancia = context.getRequiredTestInstance();
        if (!(instancia instanceof ConfiguracionE2E base) || base.driver == null) {
            return;
        }
        try {
            byte[] png = ((TakesScreenshot) base.driver).getScreenshotAs(OutputType.BYTES);
            Path directorio = Path.of("target", "e2e-screenshots");
            Files.createDirectories(directorio);
            String nombreArchivo = context.getRequiredTestClass().getSimpleName()
                    + "_" + context.getRequiredTestMethod().getName() + ".png";
            Files.write(directorio.resolve(nombreArchivo), png);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
