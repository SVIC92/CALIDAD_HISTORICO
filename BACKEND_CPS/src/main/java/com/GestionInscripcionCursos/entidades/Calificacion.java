package com.GestionInscripcionCursos.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "calificacion")
@Getter
@Setter
@NoArgsConstructor
public class Calificacion {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id_calificacion")
    private String id;

    @ManyToOne
    @JoinColumn(name = "id_inscripcion", nullable = false)
    @NotNull(message = "La inscripcion es obligatoria")
    private Inscripcion inscripcion;

    @ManyToOne
    @JoinColumn(name = "id_actividad")
    private Actividad actividad;

    @NotBlank(message = "El tipo de evaluacion es obligatorio")
    @Size(max = 60, message = "El tipo de evaluacion no debe superar 60 caracteres")
    @Column(name = "tipo_evaluacion", nullable = false, length = 60)
    private String tipoEvaluacion;

    @NotNull(message = "La nota es obligatoria")
    @Min(value = 0, message = "La nota no puede ser negativa")
    @Max(value = 20, message = "La nota no puede ser mayor a 20")
    @Column(name = "nota", precision = 5, scale = 2, nullable = false)
    private BigDecimal nota;

    @Min(value = 0, message = "El peso porcentual no puede ser negativo")
    @Max(value = 100, message = "El peso porcentual no puede ser mayor a 100")
    @Column(name = "peso_porcentual")
    private Integer pesoPorcentual;

    @Size(max = 500, message = "La observacion no debe superar 500 caracteres")
    @Column(name = "observacion", length = 500)
    private String observacion;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fecha_registro", nullable = false)
    @NotNull(message = "La fecha de registro es obligatoria")
    private Date fechaRegistro;

    @PrePersist
    protected void onCreate() {
        if (this.fechaRegistro == null) {
            this.fechaRegistro = new Date();
        }
    }

}
