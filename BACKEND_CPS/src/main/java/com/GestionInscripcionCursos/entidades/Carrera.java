package com.GestionInscripcionCursos.entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "carrera")
@Getter
@Setter
@NoArgsConstructor
public class Carrera {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id_carrera")
    private String id;

    @NotBlank(message = "El codigo de carrera es obligatorio")
    @Size(max = 20, message = "El codigo de carrera no debe superar 20 caracteres")
    @Column(name = "codigo", nullable = false, unique = true, length = 20)
    private String codigo;

    @NotBlank(message = "El nombre de carrera es obligatorio")
    @Size(max = 120, message = "El nombre de carrera no debe superar 120 caracteres")
    @Column(name = "nombre", nullable = false, unique = true)
    private String nombre;

    @Size(max = 1000, message = "La descripcion no debe superar 1000 caracteres")
    @Column(name = "descripcion", length = 1000)
    private String descripcion;

    @OneToMany(mappedBy = "carrera", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Curso> cursos = new ArrayList<>();

    @OneToMany(mappedBy = "carrera")
    @JsonIgnore
    private List<Usuario> alumnos = new ArrayList<>();

    public Carrera(String codigo, String nombre, String descripcion) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
    }
}
