package com.GestionInscripcionCursos.entidades;



import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.GenericGenerator;



@Entity
@Table(name = "curso")
public class Curso {
    
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id_curso")
    private  String id;
    

    private String nombre;
    

    private String descripcion;

    @Column(name = "capacidad_maxima")
    private Integer capacidadMaxima;

    @Column(name = "creditos")
    private Integer creditos;

    @Temporal(TemporalType.DATE)
    @Column(name = "fecha_creacion", nullable = false)
    private Date fechaCreacion;

    @Temporal(TemporalType.DATE)
    @Column(name = "fecha_termino")
    private Date fechaTermino;

    @ManyToOne
    @JoinColumn(name = "id_profesor")
    private Usuario profesorAsignado;
    
    @OneToMany(mappedBy = "curso", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Inscripcion> inscripciones;
    
    @OneToMany(mappedBy = "curso", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Actividad> actividades;

    public Curso() {
    }

    @PrePersist
    protected void onCreate() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = new Date();
        }
    }

    public Curso(String nombre, String descripcion, Integer capacidadMaxima, Integer creditos, Date fechaTermino, Usuario profesorAsignado) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.capacidadMaxima = capacidadMaxima;
        this.creditos = creditos;
        this.fechaTermino = fechaTermino;
        this.profesorAsignado = profesorAsignado;
        this.inscripciones = new ArrayList<>();
        this.actividades = new ArrayList<>();
    }
    
    

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getCapacidadMaxima() {
        return capacidadMaxima;
    }

    public void setCapacidadMaxima(Integer capacidadMaxima) {
        this.capacidadMaxima = capacidadMaxima;
    }

    public Integer getCreditos() {
        return creditos;
    }

    public void setCreditos(Integer creditos) {
        this.creditos = creditos;
    }

    public Date getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Date fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Date getFechaTermino() {
        return fechaTermino;
    }

    public void setFechaTermino(Date fechaTermino) {
        this.fechaTermino = fechaTermino;
    }

    public Usuario getProfesorAsignado() {
        return profesorAsignado;
    }

    public void setProfesorAsignado(Usuario profesorAsignado) {
        this.profesorAsignado = profesorAsignado;
    }

    @JsonProperty("profesorAsignadoNombre")
    public String getProfesorAsignadoNombre() {
        return profesorAsignado != null ? profesorAsignado.getNombre() : "Sin docente";
    }

    @JsonProperty("estado")
    public String getEstado() {
        if (fechaTermino == null) {
            return "ACTIVO";
        }

        LocalDate hoy = LocalDate.now();
        LocalDate termino = Instant.ofEpochMilli(fechaTermino.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        return hoy.isAfter(termino) ? "CADUCADO" : "ACTIVO";
    }


    public List<Inscripcion> getInscripciones() {
        return inscripciones;
    }

    public void setInscripciones(List<Inscripcion> inscripciones) {
        this.inscripciones = inscripciones;
    }

    public List<Actividad> getActividades() {
        return actividades;
    }

    public void setActividades(List<Actividad> actividades) {
        this.actividades = actividades;
    }
    
    
    
    
    
}
