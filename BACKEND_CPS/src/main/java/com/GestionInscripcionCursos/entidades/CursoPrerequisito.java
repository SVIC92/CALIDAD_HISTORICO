package com.GestionInscripcionCursos.entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(
        name = "curso_prerrequisito",
        uniqueConstraints = @UniqueConstraint(columnNames = {"id_curso", "id_prerrequisito"})
)
@Getter
@Setter
@NoArgsConstructor
public class CursoPrerequisito {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id_curso_prerrequisito")
    private String id;

    @ManyToOne
    @JoinColumn(name = "id_curso", nullable = false)
    @JsonIgnore
    @NotNull(message = "El curso destino es obligatorio")
    private Curso curso;

    @ManyToOne
    @JoinColumn(name = "id_prerrequisito", nullable = false)
    @NotNull(message = "El curso prerrequisito es obligatorio")
    private Curso prerrequisito;

    @Column(name = "obligatorio", nullable = false)
    @NotNull(message = "El indicador obligatorio es requerido")
    private Boolean obligatorio = Boolean.TRUE;

    @Size(max = 500, message = "La observacion no debe superar 500 caracteres")
    @Column(name = "observacion", length = 500)
    private String observacion;
}
