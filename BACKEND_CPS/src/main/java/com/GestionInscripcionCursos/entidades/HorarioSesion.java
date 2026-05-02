package com.GestionInscripcionCursos.entidades;

import com.GestionInscripcionCursos.enumeraciones.ModalidadCurso;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "horario_sesion")
@Getter
@Setter
@NoArgsConstructor
public class HorarioSesion {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id_horario")
    private String id;

    @ManyToOne
    @JoinColumn(name = "id_curso", nullable = false)
    @NotNull(message = "El curso es obligatorio")
    private Curso curso;

    @NotBlank(message = "El dia de la semana es obligatorio")
    @Size(max = 20, message = "El dia de la semana no debe superar 20 caracteres")
    @Column(name = "dia_semana", nullable = false, length = 20)
    private String diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    @NotNull(message = "La hora de fin es obligatoria")
    private LocalTime horaFin;

    @Size(max = 80, message = "El aula no debe superar 80 caracteres")
    @Column(name = "aula", length = 80)
    private String aula;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad", nullable = false)
    @NotNull(message = "La modalidad es obligatoria")
    private ModalidadCurso modalidad;
}
