
package com.GestionInscripcionCursos.entidades;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;


import jakarta.persistence.OneToMany;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.ArrayList;

import org.hibernate.annotations.GenericGenerator;


@Entity
@Table(name = "actividad")
public class Actividad {
    
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id_actividad")
    private  String id;
    
    @Column(name = "nombre_actividad", nullable = false)
    private String nombre;
    
    @Column(name = "descripcion", nullable = false)
    private String descripcion;
    
    @ManyToOne
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;
    
    @OneToMany(mappedBy = "actividad", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Reporte> reportes;

    public Actividad() {
    }

    public Actividad(String nombre, String descripcion, Curso curso) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.curso = curso;
        this.reportes = new ArrayList<>();
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

    public Curso getCurso() {
        return curso;
    }

    public void setCurso(Curso curso) {
        this.curso = curso;
    }

    public List<Reporte> getReportes() {
        return reportes;
    }

    public void setReportes(List<Reporte> reportes) {
        this.reportes = reportes;
    }
    
    
    
}
