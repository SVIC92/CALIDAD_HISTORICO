package com.GestionInscripcionCursos.entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "criterio_rubrica")
@Getter
@Setter
@NoArgsConstructor
public class CriterioRubrica {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String nombre;

    @Column(length = 1000)
    private String descripcion;

    private Integer peso;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "criterio_rubrica_niveles", joinColumns = @JoinColumn(name = "criterio_id"))
    @OrderColumn(name = "orden")
    private List<NivelRubrica> niveles = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "rubrica_id", nullable = false)
    @JsonIgnore
    private Rubrica rubrica;

    public int puntajeMaximoCriterio() {
        return niveles.stream().mapToInt(n -> n.getPuntaje() != null ? n.getPuntaje() : 0).max().orElse(0);
    }
}
