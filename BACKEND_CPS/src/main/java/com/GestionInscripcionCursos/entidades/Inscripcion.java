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
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
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
@Table(name = "inscripcion")
@Getter
@Setter
@NoArgsConstructor
public class Inscripcion {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id_inscripcion")
    private String id;

    @Temporal(TemporalType.DATE)
    @NotNull(message = "La fecha de creacion de la inscripcion es obligatoria")
    private Date fechaCreacion;

    @NotBlank(message = "El estado de la inscripcion es obligatorio")
    @Size(max = 30, message = "El estado no debe superar 30 caracteres")
    private String estado;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    @NotNull(message = "El usuario es obligatorio")
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "curso_id", nullable = false)
    @NotNull(message = "El curso es obligatorio")
    private Curso curso;

    @OneToMany(mappedBy = "inscripcion", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Calificacion> calificaciones = new ArrayList<>();

    public Inscripcion(Date fechaCreacion, String estado, Usuario usuario, Curso curso) {
        this.fechaCreacion = fechaCreacion;
        this.estado = estado;
        this.usuario = usuario;
        this.curso = curso;
    }
}
