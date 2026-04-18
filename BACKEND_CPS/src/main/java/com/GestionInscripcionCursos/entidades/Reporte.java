
package com.GestionInscripcionCursos.entidades;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Date;
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
    
    @NotBlank(message = "El estado del reporte es obligatorio")
    @Size(max = 30, message = "El estado no debe superar 30 caracteres")
    private String estado;
    
    @Temporal(TemporalType.DATE)
    @NotNull(message = "La fecha de creacion del reporte es obligatoria")
    private Date fechaCreacion;
    
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    @JsonIgnoreProperties({"inscripciones", "reportes", "password"})
    @NotNull(message = "El usuario del reporte es obligatorio")
    private Usuario usuario;
    
    @ManyToOne
    @JoinColumn(name = "actividad_id", nullable = false)
    @NotNull(message = "La actividad del reporte es obligatoria")
    private Actividad actividad;

    public Reporte(String respuesta, String nota, String comentario, String estado, Date fechaCreacion, Usuario usuario, Actividad actividad) {
        this.respuesta = respuesta;
        this.nota = nota;
        this.comentario = comentario;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
        this.usuario = usuario;
        this.actividad = actividad;
    }
    
}
