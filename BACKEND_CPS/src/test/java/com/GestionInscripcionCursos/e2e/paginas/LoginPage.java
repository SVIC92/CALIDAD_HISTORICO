package com.GestionInscripcionCursos.e2e.paginas;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/** Page Object de FRONTEND_CPS/src/pages/Login.jsx (ruta "/"). */
public class LoginPage {

    private static final By CAMPO_EMAIL = By.id("email");
    private static final By CAMPO_PASSWORD = By.id("password");
    private static final By BOTON_INGRESAR = By.xpath("//button[@type='submit']");

    private final WebDriver driver;
    private final WebDriverWait espera;
    private final String baseUrl;

    public LoginPage(WebDriver driver, WebDriverWait espera, String baseUrl) {
        this.driver = driver;
        this.espera = espera;
        this.baseUrl = baseUrl;
    }

    public LoginPage abrir() {
        driver.get(baseUrl + "/");
        espera.until(ExpectedConditions.visibilityOfElementLocated(CAMPO_EMAIL));
        return this;
    }

    /** Inicia sesión y espera a que el frontend redirija a un dashboard según el rol. */
    public void iniciarSesion(String email, String password) {
        driver.findElement(CAMPO_EMAIL).clear();
        driver.findElement(CAMPO_EMAIL).sendKeys(email);
        driver.findElement(CAMPO_PASSWORD).clear();
        driver.findElement(CAMPO_PASSWORD).sendKeys(password);
        driver.findElement(BOTON_INGRESAR).click();

        espera.until(ExpectedConditions.urlContains("/dashboard"));
    }
}
