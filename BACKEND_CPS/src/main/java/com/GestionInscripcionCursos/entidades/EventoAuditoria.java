package com.GestionInscripcionCursos.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "evento_auditoria")
@Getter
@Setter
@NoArgsConstructor
public class EventoAuditoria {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

    @Column(name = "tipo_evento", nullable = false)
    private String tipoEvento;

    @Column(name = "usuario_email")
    private String usuarioEmail;

    @Column(length = 500)
    private String detalle;

    @Column(nullable = false)
    private boolean exitoso;

    @Column(nullable = false)
    private LocalDateTime fecha;

    public EventoAuditoria(String tipoEvento, String usuarioEmail, String detalle, boolean exitoso, LocalDateTime fecha) {
        this.tipoEvento = tipoEvento;
        this.usuarioEmail = usuarioEmail;
        this.detalle = detalle;
        this.exitoso = exitoso;
        this.fecha = fecha;
    }
}
