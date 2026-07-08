package com.GestionInscripcionCursos.e2e.paginas;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/** Page Object de FRONTEND_CPS/src/components/Navbar.jsx (barra superior, visible en todo el dashboard). */
public class NavbarPage {

    private static final By BOTON_SALIR = By.xpath("//button[normalize-space()='Salir']");
    private static final By BOTON_CONFIRMAR_CIERRE = By.xpath("//button[normalize-space()='Cerrar sesión']");

    private final WebDriver driver;
    private final WebDriverWait espera;

    public NavbarPage(WebDriver driver, WebDriverWait espera) {
        this.driver = driver;
        this.espera = espera;
    }

    /** Cierra sesión y espera a volver a la pantalla de login ("/"). */
    public void cerrarSesion() {
        espera.until(ExpectedConditions.elementToBeClickable(BOTON_SALIR)).click();
        espera.until(ExpectedConditions.elementToBeClickable(BOTON_CONFIRMAR_CIERRE)).click();
        espera.until(ExpectedConditions.urlMatches(".*/$"));
    }
}
