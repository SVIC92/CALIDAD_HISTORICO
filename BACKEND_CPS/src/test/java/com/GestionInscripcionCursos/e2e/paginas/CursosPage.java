package com.GestionInscripcionCursos.e2e.paginas;

import com.GestionInscripcionCursos.e2e.soporte.Interacciones;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Page Object de FRONTEND_CPS/src/pages/CursosListado.jsx (ruta "/cursos/listado").
 *
 * Es una sola pantalla compartida por los tres roles: ADMIN ve botones de
 * CRUD (Horarios/Editar/Eliminar) por fila, ALUMNO/PROFESOR ven un único
 * botón de autoinscripción ("Inscribirme").
 */
public class CursosPage {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final By CAMPO_BUSCAR = By.xpath("//input[@placeholder='Buscar por nombre...']");
    private static final By BOTON_NUEVO_CURSO = By.xpath("//button[normalize-space()='Nuevo Curso']");

    private static final By CAMPO_NOMBRE = By.cssSelector("input[name='nombre']");
    private static final By CAMPO_CODIGO = By.cssSelector("input[name='codigoCurso']");
    private static final By CAMPO_DESCRIPCION =
            By.cssSelector("textarea[name='descripcion']:not([aria-hidden='true']), input[name='descripcion']");
    private static final By CAMPO_CARRERA = By.cssSelector("input[name='carrera']");
    private static final By CAMPO_CAPACIDAD = By.cssSelector("input[name='capacidadMaxima']");
    private static final By CAMPO_CREDITOS = By.cssSelector("input[name='creditos']");
    private static final By CAMPO_FECHA_TERMINO = By.cssSelector("input[name='fechaTermino']");
    private static final By CAMPO_PROFESOR_ASIGNADO = By.cssSelector("input[name='profesorAsignado']");
    private static final By BOTON_REGISTRAR = By.xpath("//button[normalize-space()='Registrar']");
    private static final By BOTON_GUARDAR_CAMBIOS = By.xpath("//button[normalize-space()='Guardar Cambios']");
    private static final By TITULO_DIALOGO_CURSO = By.xpath("//h2[normalize-space()='Registrar Curso' or normalize-space()='Editar Curso']");

    private static final By BOTON_AGREGAR_HORARIO = By.xpath("//button[normalize-space()='Agregar Horario']");
    private static final By BOTON_CERRAR_HORARIOS = By.xpath("//div[.//h2[normalize-space()='Gestión de Horarios']]//button[normalize-space()='Cerrar']");
    private static final By TITULO_DIALOGO_HORARIOS = By.xpath("//h2[normalize-space()='Gestión de Horarios']");
    private static final By LISTA_HORARIOS_REGISTRADOS = By.xpath("//h2[normalize-space()='Gestión de Horarios']/ancestor::div[contains(@class,'MuiDialog-paper')][1]");
    // Acotado al Dialog de Horarios: un locator global por label/role='combobox' puede
    // coincidir con el filtro "Estado de cursos" que queda detrás (mismo role, aunque
    // oculto por el backdrop, sigue presente en el DOM y aparece antes en orden de documento).
    private static final By CAMPO_DIA_COMBOBOX = By.xpath(
            "//h2[normalize-space()='Gestión de Horarios']/ancestor::div[contains(@class,'MuiDialog-paper')][1]"
                    + "//*[@role='combobox'][1]"); // "Día" precede a "Modalidad" en el formulario
    private static final By CAMPO_HORA_INICIO = By.xpath("//label[contains(text(),'Hora Inicio')]/ancestor::div[contains(@class,'MuiFormControl-root')][1]//input");
    private static final By CAMPO_HORA_FIN = By.xpath("//label[contains(text(),'Hora Fin')]/ancestor::div[contains(@class,'MuiFormControl-root')][1]//input");

    private static final By BOTON_ELIMINAR_CONFIRMAR = By.xpath("//button[normalize-space()='Eliminar']");
    private static final By BOTON_INSCRIBIRME_CONFIRMAR = By.xpath("//button[normalize-space()='Inscribirme']");

    // MUI Alert siempre expone role="alert" en su elemento raíz — a diferencia de las
    // clases utilitarias (MuiAlert-outlinedSuccess/Error), es estable entre versiones,
    // así que se usa como ancla en vez de depender de nombres de clase exactos.
    private static final By ALERTA_FLOTANTE = By.cssSelector("[role='alert']");
    private static final By BOTON_CERRAR_ALERTA_FLOTANTE =
            By.xpath("//*[@role='alert']/ancestor::div[contains(@class,'MuiDialog-paper')][1]//button");

    private final WebDriver driver;
    private final WebDriverWait espera;
    private final String baseUrl;

    public CursosPage(WebDriver driver, WebDriverWait espera, String baseUrl) {
        this.driver = driver;
        this.espera = espera;
        this.baseUrl = baseUrl;
    }

    public CursosPage navegar() {
        driver.get(baseUrl + "/cursos/listado");
        espera.until(ExpectedConditions.visibilityOfElementLocated(CAMPO_BUSCAR));
        return this;
    }

    public void buscar(String texto) {
        descartarMensajesFlotantes();
        WebElement campo = espera.until(ExpectedConditions.elementToBeClickable(CAMPO_BUSCAR));
        Interacciones.fijarValor(driver, campo, texto);
    }

    /**
     * FloatingMessageModal (éxito/error) no se cierra solo: queda abierto hasta que el
     * usuario hace click en su botón "X", y su backdrop de pantalla completa bloquea
     * cualquier otra interacción con la página (de ahí ElementNotInteractableException
     * al intentar usar el buscador o abrir otro diálogo justo después de crear/editar/
     * eliminar/inscribir). Se descarta defensivamente antes de la siguiente acción.
     */
    private void descartarMensajesFlotantes() {
        List<WebElement> botones = driver.findElements(BOTON_CERRAR_ALERTA_FLOTANTE);
        if (botones.isEmpty()) {
            return;
        }
        for (WebElement boton : botones) {
            try {
                clic(boton);
            } catch (Exception ignorado) {
                // pudo haberse cerrado solo por una acción previa; no es fatal.
            }
        }
        espera.until(ExpectedConditions.invisibilityOfElementLocated(ALERTA_FLOTANTE));
    }

    private By filaPorNombre(String nombre) {
        return By.xpath("//table//tr[td[normalize-space()=" + literalXPath(nombre) + "]]");
    }

    /** Envuelve el texto en comillas válidas para XPath aunque contenga comillas simples o dobles. */
    private static String literalXPath(String texto) {
        if (!texto.contains("'")) {
            return "'" + texto + "'";
        }
        return "concat('" + texto.replace("'", "', \"'\", '") + "')";
    }

    public WebElement esperarFila(String nombre) {
        buscar(nombre);
        return espera.until(ExpectedConditions.visibilityOfElementLocated(filaPorNombre(nombre)));
    }

    public void esperarAusenciaFila(String nombre) {
        buscar(nombre);
        espera.until(ExpectedConditions.invisibilityOfElementLocated(filaPorNombre(nombre)));
    }

    private WebElement botonAccion(String nombreCurso, int indice) {
        WebElement fila = esperarFila(nombreCurso);
        List<WebElement> botones = fila.findElements(By.xpath(".//td[last()]//button"));
        return botones.get(indice);
    }

    /**
     * Click "de verdad" (WebElement.click()) sobre los IconButton de la columna Acciones
     * demostró ser poco confiable en esta suite: el elemento se reportaba clickable pero
     * el manejador de React nunca se disparaba (el diálogo de confirmación jamás se
     * abría), sin lanzar ninguna excepción — probablemente por el ripple/Tooltip de MUI
     * interceptando el punto de clic. Disparar el evento por JS va directo al elemento y
     * es consistente en esta pantalla.
     */
    private void clic(WebElement elemento) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", elemento);
    }

    private void clicAccion(String nombreCurso, int indice) {
        clic(botonAccion(nombreCurso, indice));
    }

    // ---- Flujo ADMIN: alta / edición / eliminación ----

    public void abrirNuevoCurso() {
        descartarMensajesFlotantes();
        clic(espera.until(ExpectedConditions.elementToBeClickable(BOTON_NUEVO_CURSO)));
        espera.until(ExpectedConditions.visibilityOfElementLocated(TITULO_DIALOGO_CURSO));
    }

    /**
     * Completa y guarda el formulario de alta. El resto de campos ya traen defaults válidos.
     * "codigoCurso" se envía explícito (no se deja en blanco): si se omite, el backend lo
     * autogenera truncando "nombre" (ver normalizarCodigoCurso en CursoServicio.java), y como
     * todos los nombres de esta suite comparten prefijo, ese código autogenerado siempre
     * coincide y choca con cursos huérfanos de corridas anteriores.
     */
    public void registrarCurso(String nombre, String codigoCurso, String descripcion, String carrera,
                                int capacidadMaxima, int creditos, LocalDate fechaTermino) {
        registrarCurso(nombre, codigoCurso, descripcion, carrera, capacidadMaxima, creditos, fechaTermino, null);
    }

    /**
     * Sobrecarga que además asigna un docente (email o id de una cuenta PROFESOR ya
     * existente) al registrar. Necesaria para flujos que luego inscriben un ALUMNO: desde
     * que CursoServicio.inscribirCurso valida profesorAsignado != null, un curso sin
     * docente ya no admite autoinscripción de alumnos.
     */
    public void registrarCurso(String nombre, String codigoCurso, String descripcion, String carrera,
                                int capacidadMaxima, int creditos, LocalDate fechaTermino, String profesorAsignado) {
        completarDatosBasicos(nombre, codigoCurso, descripcion, carrera, capacidadMaxima, creditos, fechaTermino);
        if (profesorAsignado != null && !profesorAsignado.isBlank()) {
            Interacciones.fijarValor(driver, driver.findElement(CAMPO_PROFESOR_ASIGNADO), profesorAsignado);
        }
        clic(espera.until(ExpectedConditions.elementToBeClickable(BOTON_REGISTRAR)));
        espera.until(ExpectedConditions.invisibilityOfElementLocated(TITULO_DIALOGO_CURSO));
    }

    public void editarNombre(String nombreActual, String nombreNuevo) {
        clicAccion(nombreActual, 1); // índice 1 = "Editar"
        espera.until(ExpectedConditions.visibilityOfElementLocated(TITULO_DIALOGO_CURSO));
        Interacciones.fijarValor(driver, driver.findElement(CAMPO_NOMBRE), nombreNuevo);
        // Un curso sin profesor precarga este campo con el sentinela "Sin docente"; si se
        // reenvía tal cual, el backend lo trata como una referencia real y responde
        // "No se encontro el profesor asignado". Se limpia para no depender de ese valor.
        Interacciones.fijarValor(driver, driver.findElement(CAMPO_PROFESOR_ASIGNADO), "");
        clic(espera.until(ExpectedConditions.elementToBeClickable(BOTON_GUARDAR_CAMBIOS)));
        espera.until(ExpectedConditions.invisibilityOfElementLocated(TITULO_DIALOGO_CURSO));
    }

    /**
     * "Eliminar" es un soft-delete (CursoServicio marca estado=INACTIVO, no borra la fila) —
     * la fila sigue en la tabla, así que se confirma por el mensaje de éxito, no por su
     * ausencia. Ver estadoDeFila() para comprobar que quedó en INACTIVO.
     */
    public void eliminarCurso(String nombre) {
        clicAccion(nombre, 2); // índice 2 = "Eliminar"
        clic(espera.until(ExpectedConditions.elementToBeClickable(BOTON_ELIMINAR_CONFIRMAR)));
        String mensaje = esperarMensajeExito();
        if (!mensaje.contains("eliminado")) {
            throw new IllegalStateException("Mensaje inesperado tras eliminar: " + mensaje);
        }
    }

    /** Texto de la columna "Estado" (índice 5: Nombre, Descripción, Capacidad, Créditos, Fecha Término, Estado). */
    public String estadoDeFila(String nombre) {
        WebElement fila = esperarFila(nombre);
        List<WebElement> celdas = fila.findElements(By.tagName("td"));
        return celdas.get(5).getText();
    }

    private void completarDatosBasicos(String nombre, String codigoCurso, String descripcion, String carrera,
                                        int capacidadMaxima, int creditos, LocalDate fechaTermino) {
        Interacciones.fijarValor(driver, driver.findElement(CAMPO_NOMBRE), nombre);
        Interacciones.fijarValor(driver, driver.findElement(CAMPO_CODIGO), codigoCurso);
        Interacciones.fijarValor(driver, driver.findElement(CAMPO_DESCRIPCION), descripcion);
        Interacciones.fijarValor(driver, driver.findElement(CAMPO_CARRERA), carrera);
        Interacciones.fijarValor(driver, driver.findElement(CAMPO_CAPACIDAD), String.valueOf(capacidadMaxima));
        Interacciones.fijarValor(driver, driver.findElement(CAMPO_CREDITOS), String.valueOf(creditos));
        Interacciones.fijarValor(driver, driver.findElement(CAMPO_FECHA_TERMINO), fechaTermino.format(FORMATO_FECHA));
    }

    // ---- Flujo ADMIN: horarios (RF05 depende de esto) ----

    public void abrirHorarios(String nombreCurso) {
        clicAccion(nombreCurso, 0); // índice 0 = "Horarios"
        espera.until(ExpectedConditions.visibilityOfElementLocated(TITULO_DIALOGO_HORARIOS));
    }

    /**
     * Selecciona el día en el combo "Día" del formulario de horarios (MUI Select, no
     * &lt;select&gt; nativo) por POSICIÓN (1=Lunes … 7=Domingo), no por texto: evita
     * depender de que tildes como "Miércoles"/"Sábado" viajen intactas entre el
     * literal Java y el DOM real del navegador.
     *
     * A diferencia de clic() (JS puro): MUI Select no abre su menú con un evento
     * "click" sintético aislado — necesita la secuencia real mousedown/mouseup/click
     * que WebElement.click() sí dispara, así que aquí se usa el click nativo.
     */
    public void seleccionarDia(int posicion) {
        espera.until(ExpectedConditions.elementToBeClickable(CAMPO_DIA_COMBOBOX)).click();
        By opcion = By.xpath("(//li[@role='option'])[" + posicion + "]");
        espera.until(ExpectedConditions.elementToBeClickable(opcion)).click();
    }

    /** Agrega un horario con día (1=Lunes…7=Domingo) y horas indicados. Horas en formato "HH:mm" (24h). */
    public void agregarHorario(int diaPosicion, String horaInicio, String horaFin) {
        seleccionarDia(diaPosicion);
        Interacciones.fijarValor(driver, driver.findElement(CAMPO_HORA_INICIO), horaInicio);
        Interacciones.fijarValor(driver, driver.findElement(CAMPO_HORA_FIN), horaFin);
        clic(espera.until(ExpectedConditions.elementToBeClickable(BOTON_AGREGAR_HORARIO)));
        espera.until(ExpectedConditions.textToBePresentInElementLocated(LISTA_HORARIOS_REGISTRADOS, horaInicio));
    }

    public void cerrarHorarios() {
        descartarMensajesFlotantes();
        clic(espera.until(ExpectedConditions.elementToBeClickable(BOTON_CERRAR_HORARIOS)));
        espera.until(ExpectedConditions.invisibilityOfElementLocated(TITULO_DIALOGO_HORARIOS));
    }

    // ---- Flujo ALUMNO/PROFESOR: autoinscripción ----

    /** Click en "Inscribirme" (único botón de acción para ALUMNO/PROFESOR) y confirma el modal. */
    public void inscribirme(String nombreCurso) {
        clicAccion(nombreCurso, 0);
        clic(espera.until(ExpectedConditions.elementToBeClickable(BOTON_INSCRIBIRME_CONFIRMAR)));
    }

    // ---- Mensajes flotantes (FloatingMessageModal) ----

    // No se distingue error/éxito por clase CSS de severidad (ver ALERTA_FLOTANTE):
    // ambos métodos esperan la misma alerta visible; el texto esperado lo valida el test.
    public String esperarMensajeError() {
        return espera.until(ExpectedConditions.visibilityOfElementLocated(ALERTA_FLOTANTE)).getText();
    }

    public String esperarMensajeExito() {
        return espera.until(ExpectedConditions.visibilityOfElementLocated(ALERTA_FLOTANTE)).getText();
    }
}
