package com.GestionInscripcionCursos.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class NivelRubrica {

    @Column(name = "nivel_nombre")
    private String nombre;

    @Column(name = "nivel_puntaje")
    private Integer puntaje;

    @Column(name = "nivel_descriptor", length = 1000)
    private String descriptor;

    public NivelRubrica(String nombre, Integer puntaje, String descriptor) {
        this.nombre = nombre;
        this.puntaje = puntaje;
        this.descriptor = descriptor;
    }
}
