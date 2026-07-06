package com.GestionInscripcionCursos.entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

/**
 * Puntaje obtenido por un alumno en un criterio puntual de la rubrica
 * aplicada a su {@link Reporte} (RF-09).
 */
@Entity
@Table(name = "puntaje_criterio")
@Getter
@Setter
@NoArgsConstructor
public class PuntajeCriterio {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

    @ManyToOne
    @JoinColumn(name = "reporte_id", nullable = false)
    @JsonIgnore
    private Reporte reporte;

    @ManyToOne
    @JoinColumn(name = "criterio_id", nullable = false)
    private CriterioRubrica criterio;

    @Column(name = "puntaje_obtenido", nullable = false)
    private Integer puntajeObtenido;

    public PuntajeCriterio(Reporte reporte, CriterioRubrica criterio, Integer puntajeObtenido) {
        this.reporte = reporte;
        this.criterio = criterio;
        this.puntajeObtenido = puntajeObtenido;
    }
}
