package com.GestionInscripcionCursos.entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.GestionInscripcionCursos.enumeraciones.Rol;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Date;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id_usuario")
    protected String id;

    @Column(name = "nombre_usuario", nullable = false)
    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(max = 120, message = "El nombre de usuario no debe superar 120 caracteres")
    protected String nombre;

    @Column(name = "email", nullable = false, unique = true)
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe tener formato valido")
    @Size(max = 255, message = "El email no debe superar 255 caracteres")
    protected String email;
    
    @Column(name = "password", nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(message = "La contrasena es obligatoria")
    @Size(min = 6, max = 255, message = "La contrasena debe tener entre 6 y 255 caracteres")
    protected String password;

    @Column(name = "two_factor_enabled")
    private Boolean twoFactorEnabled;

    @Column(name = "two_factor_secret")
    @JsonIgnore
    private String twoFactorSecret;

    @Temporal(TemporalType.DATE)
    @NotNull(message = "La fecha de creacion es obligatoria")
    private Date fechaCreacion;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "El rol del usuario es obligatorio")
    protected Rol rol;

    @ManyToOne
    @JoinColumn(name = "id_carrera")
    private Carrera carrera;

    @Column(name = "ciclo_actual")
    @Min(value = 1, message = "El ciclo actual debe ser mayor o igual a 1")
    @Max(value = 14, message = "El ciclo actual no debe superar 14")
    private Integer cicloActual;
    
    
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Inscripcion> inscripciones;
    
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Reporte> reportes;

    @PrePersist
    protected void onCreate() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = new Date();
        }
    }

    public Usuario(String id, String nombre, String email, String password, Date fechaCreacion, Rol rol, List<Inscripcion> inscripciones, List<Reporte> reportes) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.password = password;
        this.fechaCreacion = fechaCreacion;
        this.rol = rol;
        this.inscripciones = inscripciones;
        this.reportes = reportes;
    }

    public boolean isTwoFactorEnabled() {
        return Boolean.TRUE.equals(twoFactorEnabled);
    }

    public void setTwoFactorEnabled(boolean twoFactorEnabled) {
        this.twoFactorEnabled = twoFactorEnabled;
    }

    public String getTwoFactorSecret() {
        return twoFactorSecret;
    }

    public void setTwoFactorSecret(String twoFactorSecret) {
        this.twoFactorSecret = twoFactorSecret;
    }
    
    

}
