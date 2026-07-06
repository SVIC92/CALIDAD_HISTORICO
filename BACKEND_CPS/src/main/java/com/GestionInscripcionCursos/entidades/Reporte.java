
package com.GestionInscripcionCursos.entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.GestionInscripcionCursos.enumeraciones.EstadoEntrega;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "reporte")
@Getter
@Setter
@NoArgsConstructor
public class Reporte {
    
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id_reporte")
    private String id;
    
    @NotBlank(message = "La respuesta del reporte es obligatoria")
    @Size(max = 8000, message = "La respuesta no debe superar 8000 caracteres")
    private String respuesta;
    
    @NotBlank(message = "La nota del reporte es obligatoria")
    @Size(max = 20, message = "La nota no debe superar 20 caracteres")
    private String nota;
    
    @Size(max = 1000, message = "El comentario no debe superar 1000 caracteres")
    private String comentario;
    
    @NotNull(message = "El estado del reporte es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private EstadoEntrega estado;

    @Temporal(TemporalType.DATE)
    @NotNull(message = "La fecha de creacion del reporte es obligatoria")
    private Date fechaCreacion;

    @Column(name = "archivo_url", length = 1000)
    private String archivoUrl;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    @JsonIgnoreProperties({"inscripciones", "reportes", "password"})
    @NotNull(message = "El usuario del reporte es obligatorio")
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "actividad_id", nullable = false)
    @NotNull(message = "La actividad del reporte es obligatoria")
    private Actividad actividad;

    @OneToMany(mappedBy = "reporte", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<PuntajeCriterio> puntajesCriterio = new ArrayList<>();

    public Reporte(String respuesta, String nota, String comentario, EstadoEntrega estado, Date fechaCreacion, Usuario usuario, Actividad actividad) {
        this.respuesta = respuesta;
        this.nota = nota;
        this.comentario = comentario;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
        this.usuario = usuario;
        this.actividad = actividad;
        this.archivoUrl = null;
    }

}
