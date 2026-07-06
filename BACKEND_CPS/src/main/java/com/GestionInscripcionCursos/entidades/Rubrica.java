package com.GestionInscripcionCursos.entidades;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

/**
 * Rubrica generada (con o sin ayuda de IA) y persistida para poder vincularla
 * a una {@link Actividad} y usarla para calificar entregas (RF-09/RF-10).
 */
@Entity
@Table(name = "rubrica")
@Getter
@Setter
@NoArgsConstructor
public class Rubrica {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

    private String titulo;

    @Column(length = 1000)
    private String descripcion;

    private String tema;

    @Column(name = "nivel_educativo")
    private String nivelEducativo;

    private String asignatura;

    @Column(name = "tipo_tarea")
    private String tipoTarea;

    @Column(name = "puntaje_maximo")
    private Integer puntajeMaximo;

    @Column(name = "generada_por_ia")
    private boolean generadaPorIa;

    private String modelo;

    @Column(name = "fecha_generacion")
    private LocalDateTime fechaGeneracion;

    @ManyToOne
    @JoinColumn(name = "curso_id", nullable = false)
    @JsonIgnoreProperties({"prerequisitos", "profesorAsignado"})
    private Curso curso;

    @ManyToOne
    @JoinColumn(name = "creado_por_id", nullable = false)
    @JsonIgnoreProperties({"inscripciones", "reportes", "password"})
    private Usuario creadoPor;

    @OneToMany(mappedBy = "rubrica", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CriterioRubrica> criterios = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.fechaGeneracion == null) {
            this.fechaGeneracion = LocalDateTime.now();
        }
    }
}
