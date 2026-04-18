package com.GestionInscripcionCursos.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "ia_historial")
@Getter
@Setter
@NoArgsConstructor
public class IaHistorial {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id_historial")
    private String id;

    @OneToOne(optional = false)
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    @NotNull(message = "El usuario es obligatorio")
    private Usuario usuario;

    @Column(name = "ultimo_mensaje", nullable = false, length = 4000)
    @NotBlank(message = "El ultimo mensaje es obligatorio")
    @Size(max = 4000, message = "El ultimo mensaje no debe superar 4000 caracteres")
    private String ultimoMensaje;

    @Column(name = "ultima_respuesta", nullable = false, length = 8000)
    @NotBlank(message = "La ultima respuesta es obligatoria")
    @Size(max = 8000, message = "La ultima respuesta no debe superar 8000 caracteres")
    private String ultimaRespuesta;

    @Column(name = "rol", nullable = false, length = 30)
    @NotBlank(message = "El rol es obligatorio")
    @Size(max = 30, message = "El rol no debe superar 30 caracteres")
    private String rol;

    @Column(name = "modelo", nullable = false, length = 100)
    @NotBlank(message = "El modelo es obligatorio")
    @Size(max = 100, message = "El modelo no debe superar 100 caracteres")
    private String modelo;

    @Column(name = "fecha_actualizacion", nullable = false)
    @NotNull(message = "La fecha de actualizacion es obligatoria")
    private Instant fechaActualizacion;
}
