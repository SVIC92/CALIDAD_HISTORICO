
package com.GestionInscripcionCursos.entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "actividad")
@Getter
@Setter
@NoArgsConstructor
public class Actividad {
    
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id_actividad")
    private String id;
    
    @NotBlank(message = "El nombre de la actividad es obligatorio")
    @Size(max = 120, message = "El nombre de la actividad no debe superar 120 caracteres")
    @Column(name = "nombre_actividad", nullable = false)
    private String nombre;
    
    @NotBlank(message = "La descripcion de la actividad es obligatoria")
    @Size(max = 1000, message = "La descripcion no debe superar 1000 caracteres")
    @Column(name = "descripcion", nullable = false)
    private String descripcion;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fecha_creacion", nullable = false)
    private Date fechaCreacion;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fecha_vencimiento", nullable = false)
    @NotNull(message = "La fecha de vencimiento es obligatoria")
    @Future(message = "La fecha de vencimiento debe estar en el futuro")
    private Date fechaVencimiento;
    
    @ManyToOne
    @JoinColumn(name = "curso_id", nullable = false)
    @NotNull(message = "El curso de la actividad es obligatorio")
    private Curso curso;
    
    @OneToMany(mappedBy = "actividad", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Reporte> reportes;

    @PrePersist
    protected void onCreate() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = new Date();
        }
    }

    public Actividad(String nombre, String descripcion, Date fechaVencimiento, Curso curso) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.fechaVencimiento = fechaVencimiento;
        this.curso = curso;
        this.reportes = new ArrayList<>();
    }
}
