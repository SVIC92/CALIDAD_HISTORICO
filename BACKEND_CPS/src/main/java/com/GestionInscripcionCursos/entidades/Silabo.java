package com.GestionInscripcionCursos.entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Date;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "silabo")
@Getter
@Setter
@NoArgsConstructor
public class Silabo {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id_silabo")
    private String id;

    @OneToOne
    @JoinColumn(name = "id_curso", nullable = false, unique = true)
    @JsonIgnore
    @NotNull(message = "El curso del silabo es obligatorio")
    private Curso curso;

    @Size(max = 4000, message = "La sumilla no debe superar 4000 caracteres")
    @Column(name = "sumilla", columnDefinition = "TEXT")
    private String sumilla;

    @Size(max = 8000, message = "Las competencias generales no deben superar 8000 caracteres")
    @Column(name = "competencias_generales", columnDefinition = "TEXT")
    private String competenciasGenerales;

    @Size(max = 8000, message = "Las competencias especificas no deben superar 8000 caracteres")
    @Column(name = "competencias_especificas", columnDefinition = "TEXT")
    private String competenciasEspecificas;

    @Size(max = 4000, message = "El logro del curso no debe superar 4000 caracteres")
    @Column(name = "logro_curso", columnDefinition = "TEXT")
    private String logroCurso;

    @Size(max = 4000, message = "El sistema de evaluacion no debe superar 4000 caracteres")
    @Column(name = "sistema_evaluacion", columnDefinition = "TEXT")
    private String sistemaEvaluacion;

    @Size(max = 12000, message = "El contenido semanal no debe superar 12000 caracteres")
    @Column(name = "contenido_semanal", columnDefinition = "TEXT")
    private String contenidoSemanal;

    @Size(max = 50, message = "La version no debe superar 50 caracteres")
    @Column(name = "version", length = 50)
    private String version;

    @Temporal(TemporalType.DATE)
    @Column(name = "fecha_aprobacion")
    private Date fechaAprobacion;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fecha_creacion", nullable = false)
    @NotNull(message = "La fecha de creacion del silabo es obligatoria")
    private Date fechaCreacion;

    @PrePersist
    protected void onCreate() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = new Date();
        }
    }

}
