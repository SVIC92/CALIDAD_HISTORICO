package com.GestionInscripcionCursos.entidades;



import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.GestionInscripcionCursos.enumeraciones.EstadoCurso;
import com.GestionInscripcionCursos.enumeraciones.ModalidadCurso;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;



@Entity
@Table(name = "curso")
@Getter
@Setter
@NoArgsConstructor
public class Curso {
    
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id_curso")
    private  String id;
    

    @NotBlank(message = "El nombre del curso es obligatorio")
    @Size(max = 150, message = "El nombre del curso no debe superar 150 caracteres")
    private String nombre;
    
    @Column(name = "codigo_curso", nullable = false, unique = true, length = 30)
    @NotBlank(message = "El codigo del curso es obligatorio")
    @Size(max = 30, message = "El codigo del curso no debe superar 30 caracteres")
    private String codigoCurso;

    @NotBlank(message = "La descripcion del curso es obligatoria")
    @Size(max = 1000, message = "La descripcion del curso no debe superar 1000 caracteres")
    private String descripcion;

    @Column(name = "ciclo")
    @NotNull(message = "El ciclo es obligatorio")
    @Min(value = 1, message = "El ciclo debe ser mayor o igual a 1")
    @Max(value = 14, message = "El ciclo no debe superar 14")
    private Integer ciclo;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad", nullable = false)
    @NotNull(message = "La modalidad del curso es obligatoria")
    private ModalidadCurso modalidad;

    @Column(name = "capacidad_maxima")
    @NotNull(message = "La capacidad maxima es obligatoria")
    @Min(value = 1, message = "La capacidad maxima debe ser mayor a 0")
    @Max(value = 40, message = "La capacidad maxima no debe superar 40")
    private Integer capacidadMaxima;

    @Column(name = "creditos")
    @NotNull(message = "Los creditos son obligatorios")
    @Min(value = 1, message = "Los creditos deben ser mayor a 0")
    @Max(value = 8, message = "Los creditos no deben superar 8")
    private Integer creditos;

    @Temporal(TemporalType.DATE)
    @Column(name = "fecha_creacion", nullable = false)
    private Date fechaCreacion;

    @Temporal(TemporalType.DATE)
    @Column(name = "fecha_termino")
    @NotNull(message = "La fecha de termino es obligatoria")
    private Date fechaTermino;

    @Temporal(TemporalType.DATE)
    @Column(name = "fecha_inicio")
    @NotNull(message = "La fecha de inicio es obligatoria")
    private Date fechaInicio;

    @Column(name = "horas_teoricas")
    @Min(value = 0, message = "Las horas teoricas no pueden ser negativas")
    @Max(value = 20, message = "Las horas teoricas no deben superar 20")
    private Integer horasTeoricas;

    @Column(name = "horas_practicas")
    @Min(value = 0, message = "Las horas practicas no pueden ser negativas")
    @Max(value = 20, message = "Las horas practicas no deben superar 20")
    private Integer horasPracticas;

    @Column(name = "horas_laboratorio")
    @Min(value = 0, message = "Las horas de laboratorio no pueden ser negativas")
    @Max(value = 20, message = "Las horas de laboratorio no deben superar 20")
    private Integer horasLaboratorio;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    @NotNull(message = "El estado del curso es obligatorio")
    private EstadoCurso estado;

    @ManyToOne
    @JoinColumn(name = "id_profesor")
    private Usuario profesorAsignado;

    @ManyToOne
    @JoinColumn(name = "id_carrera")
    @NotNull(message = "La carrera del curso es obligatoria")
    private Carrera carrera;
    
    @OneToMany(mappedBy = "curso", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Inscripcion> inscripciones;
    
    @OneToMany(mappedBy = "curso", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Actividad> actividades;

    @OneToOne(mappedBy = "curso", cascade = CascadeType.ALL, orphanRemoval = true)
    private Silabo silabo;

    @OneToMany(mappedBy = "curso", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<HorarioSesion> sesionesHorario;

    @OneToMany(mappedBy = "curso", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<CursoPrerequisito> prerequisitos;

    @OneToMany(mappedBy = "prerrequisito", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<CursoPrerequisito> esPrerrequisitoDe;

    @PrePersist
    protected void onCreate() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = new Date();
        }
        if (this.estado == null) {
            this.estado = EstadoCurso.ACTIVO;
        }
        if (this.modalidad == null) {
            this.modalidad = ModalidadCurso.PRESENCIAL;
        }
    }

    public Curso(String nombre, String descripcion, Integer capacidadMaxima, Integer creditos, Date fechaTermino, Usuario profesorAsignado) {
        this(
                nombre,
                nombre != null ? nombre.replaceAll("\\s+", "").toUpperCase() : null,
                descripcion,
                1,
                ModalidadCurso.PRESENCIAL,
                capacidadMaxima,
                creditos,
                new Date(),
                fechaTermino,
                0,
                0,
                0,
                EstadoCurso.ACTIVO,
                profesorAsignado,
                null
        );
    }

    public Curso(String nombre, String codigoCurso, String descripcion, Integer ciclo, ModalidadCurso modalidad, Integer capacidadMaxima, Integer creditos, Date fechaInicio, Date fechaTermino, Integer horasTeoricas, Integer horasPracticas, Integer horasLaboratorio, EstadoCurso estado, Usuario profesorAsignado, Carrera carrera) {
        this.nombre = nombre;
        this.codigoCurso = codigoCurso;
        this.descripcion = descripcion;
        this.ciclo = ciclo;
        this.modalidad = modalidad;
        this.capacidadMaxima = capacidadMaxima;
        this.creditos = creditos;
        this.fechaInicio = fechaInicio;
        this.fechaTermino = fechaTermino;
        this.horasTeoricas = horasTeoricas;
        this.horasPracticas = horasPracticas;
        this.horasLaboratorio = horasLaboratorio;
        this.estado = estado;
        this.profesorAsignado = profesorAsignado;
        this.carrera = carrera;
        this.inscripciones = new ArrayList<>();
        this.actividades = new ArrayList<>();
        this.sesionesHorario = new ArrayList<>();
        this.prerequisitos = new ArrayList<>();
        this.esPrerrequisitoDe = new ArrayList<>();
    }
    
    

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCodigoCurso() {
        return codigoCurso;
    }

    public void setCodigoCurso(String codigoCurso) {
        this.codigoCurso = codigoCurso;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getCiclo() {
        return ciclo;
    }

    public void setCiclo(Integer ciclo) {
        this.ciclo = ciclo;
    }

    public ModalidadCurso getModalidad() {
        return modalidad;
    }

    public void setModalidad(ModalidadCurso modalidad) {
        this.modalidad = modalidad;
    }

    public Integer getCapacidadMaxima() {
        return capacidadMaxima;
    }

    public void setCapacidadMaxima(Integer capacidadMaxima) {
        this.capacidadMaxima = capacidadMaxima;
    }

    public Integer getCreditos() {
        return creditos;
    }

    public void setCreditos(Integer creditos) {
        this.creditos = creditos;
    }

    public Date getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Date fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Date getFechaTermino() {
        return fechaTermino;
    }

    public void setFechaTermino(Date fechaTermino) {
        this.fechaTermino = fechaTermino;
    }

    public Date getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(Date fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public Integer getHorasTeoricas() {
        return horasTeoricas;
    }

    public void setHorasTeoricas(Integer horasTeoricas) {
        this.horasTeoricas = horasTeoricas;
    }

    public Integer getHorasPracticas() {
        return horasPracticas;
    }

    public void setHorasPracticas(Integer horasPracticas) {
        this.horasPracticas = horasPracticas;
    }

    public Integer getHorasLaboratorio() {
        return horasLaboratorio;
    }

    public void setHorasLaboratorio(Integer horasLaboratorio) {
        this.horasLaboratorio = horasLaboratorio;
    }

    public EstadoCurso getEstado() {
        return estado;
    }

    public void setEstado(EstadoCurso estado) {
        this.estado = estado;
    }

    public Usuario getProfesorAsignado() {
        return profesorAsignado;
    }

    public void setProfesorAsignado(Usuario profesorAsignado) {
        this.profesorAsignado = profesorAsignado;
    }

    public Carrera getCarrera() {
        return carrera;
    }

    public void setCarrera(Carrera carrera) {
        this.carrera = carrera;
    }

    @JsonProperty("profesorAsignadoNombre")
    public String getProfesorAsignadoNombre() {
        return profesorAsignado != null ? profesorAsignado.getNombre() : "Sin docente";
    }


    public List<Inscripcion> getInscripciones() {
        return inscripciones;
    }

    public void setInscripciones(List<Inscripcion> inscripciones) {
        this.inscripciones = inscripciones;
    }

    public List<Actividad> getActividades() {
        return actividades;
    }

    public void setActividades(List<Actividad> actividades) {
        this.actividades = actividades;
    }

    public Silabo getSilabo() {
        return silabo;
    }

    public void setSilabo(Silabo silabo) {
        this.silabo = silabo;
    }

    public List<HorarioSesion> getSesionesHorario() {
        return sesionesHorario;
    }

    public void setSesionesHorario(List<HorarioSesion> sesionesHorario) {
        this.sesionesHorario = sesionesHorario;
    }

    public List<CursoPrerequisito> getPrerequisitos() {
        return prerequisitos;
    }

    public void setPrerequisitos(List<CursoPrerequisito> prerequisitos) {
        this.prerequisitos = prerequisitos;
    }

    public List<CursoPrerequisito> getEsPrerrequisitoDe() {
        return esPrerrequisitoDe;
    }

    public void setEsPrerrequisitoDe(List<CursoPrerequisito> esPrerrequisitoDe) {
        this.esPrerrequisitoDe = esPrerrequisitoDe;
    }
    
    
    
    
    
}
