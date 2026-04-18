package com.GestionInscripcionCursos.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "password_reset_token")
@Getter
@Setter
@NoArgsConstructor
public class PasswordResetToken {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "El token de recuperacion es obligatorio")
    @Size(max = 255, message = "El token no debe superar 255 caracteres")
    private String token;

    @Column(nullable = false)
    @NotNull(message = "La fecha de expiracion es obligatoria")
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used;

    @Column(nullable = false)
    @NotNull(message = "La fecha de creacion es obligatoria")
    private LocalDateTime createdAt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    @NotNull(message = "El usuario del token es obligatorio")
    private Usuario usuario;

    public PasswordResetToken(String token, LocalDateTime expiresAt, boolean used, LocalDateTime createdAt, Usuario usuario) {
        this.token = token;
        this.expiresAt = expiresAt;
        this.used = used;
        this.createdAt = createdAt;
        this.usuario = usuario;
    }
}
