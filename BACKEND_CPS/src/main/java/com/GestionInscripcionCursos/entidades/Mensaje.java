package com.GestionInscripcionCursos.entidades;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import java.util.Date;

@Entity
@Table(name = "mensaje")
@Getter
@Setter
@NoArgsConstructor
public class Mensaje {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

    @ManyToOne
    @JoinColumn(name = "emisor_id", nullable = false)
    private Usuario emisor;

    @ManyToOne
    @JoinColumn(name = "receptor_id", nullable = false)
    private Usuario receptor;

    @Column(nullable = false, length = 1000)
    private String contenido;

    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaEnvio;

    private boolean leido = false;

    // Puede ser "TEXTO", "IMAGEN", "PDF", "WORD", etc.
    @Column(nullable = false)
    private String tipo = "TEXTO"; 

    // Guardará la URL de Cloudinary
    @Column(length = 500)
    private String urlArchivo;

    @PrePersist
    protected void onCreate() {
        if (this.fechaEnvio == null) {
            this.fechaEnvio = new Date();
        }
    }
}
