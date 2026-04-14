package com.GestionInscripcionCursos.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "ia_historial")
public class IaHistorial {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id_historial")
    private String id;

    @OneToOne(optional = false)
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "ultimo_mensaje", nullable = false, length = 4000)
    private String ultimoMensaje;

    @Column(name = "ultima_respuesta", nullable = false, length = 8000)
    private String ultimaRespuesta;

    @Column(name = "rol", nullable = false, length = 30)
    private String rol;

    @Column(name = "modelo", nullable = false, length = 100)
    private String modelo;

    @Column(name = "fecha_actualizacion", nullable = false)
    private Instant fechaActualizacion;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getUltimoMensaje() {
        return ultimoMensaje;
    }

    public void setUltimoMensaje(String ultimoMensaje) {
        this.ultimoMensaje = ultimoMensaje;
    }

    public String getUltimaRespuesta() {
        return ultimaRespuesta;
    }

    public void setUltimaRespuesta(String ultimaRespuesta) {
        this.ultimaRespuesta = ultimaRespuesta;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public Instant getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(Instant fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
}
