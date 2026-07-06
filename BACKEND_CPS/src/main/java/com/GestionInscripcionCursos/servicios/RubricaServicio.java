package com.GestionInscripcionCursos.servicios;

import com.GestionInscripcionCursos.dto.RubricaGeneradaDto;
import com.GestionInscripcionCursos.entidades.Curso;
import com.GestionInscripcionCursos.entidades.CriterioRubrica;
import com.GestionInscripcionCursos.entidades.NivelRubrica;
import com.GestionInscripcionCursos.entidades.Rubrica;
import com.GestionInscripcionCursos.entidades.Usuario;
import com.GestionInscripcionCursos.excepciones.MyException;
import com.GestionInscripcionCursos.repositorios.CursoRepositorio;
import com.GestionInscripcionCursos.repositorios.RubricaRepositorio;
import com.GestionInscripcionCursos.repositorios.UsuarioRepositorio;

import java.util.ArrayList;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

/**
 * Persiste las rubricas generadas (con o sin IA) vinculandolas a un curso,
 * cerrando el hueco donde antes solo se mostraban en pantalla y se
 * descartaban (RF-10).
 */
@Service
public class RubricaServicio {

    private final RubricaRepositorio rubricaRepositorio;
    private final CursoRepositorio cursoRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;

    public RubricaServicio(
            RubricaRepositorio rubricaRepositorio,
            CursoRepositorio cursoRepositorio,
            UsuarioRepositorio usuarioRepositorio
    ) {
        this.rubricaRepositorio = rubricaRepositorio;
        this.cursoRepositorio = cursoRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
    }

    @Transactional
    public Rubrica guardarDesdeGeneracion(RubricaGeneradaDto dto, String cursoId, String emailCreador) throws MyException {
        if (dto == null || dto.criterios() == null || dto.criterios().isEmpty()) {
            throw new MyException("La rúbrica debe tener al menos un criterio");
        }
        if (cursoId == null || cursoId.isBlank()) {
            throw new MyException("El curso es obligatorio para guardar la rúbrica");
        }

        Curso curso = cursoRepositorio.findById(cursoId)
                .orElseThrow(() -> new MyException("Curso no encontrado"));
        Usuario creador = usuarioRepositorio.buscarPorEmail(emailCreador);
        if (creador == null) {
            throw new MyException("Usuario no encontrado");
        }

        Rubrica rubrica = new Rubrica();
        rubrica.setTitulo(dto.titulo());
        rubrica.setDescripcion(dto.descripcion());
        rubrica.setTema(dto.tema());
        rubrica.setNivelEducativo(dto.nivelEducativo());
        rubrica.setAsignatura(dto.asignatura());
        rubrica.setTipoTarea(dto.tipoTarea());
        rubrica.setPuntajeMaximo(dto.puntajeMaximo());
        rubrica.setGeneradaPorIa(dto.generadaPorIa());
        rubrica.setModelo(dto.modelo());
        rubrica.setCurso(curso);
        rubrica.setCreadoPor(creador);

        List<CriterioRubrica> criterios = new ArrayList<>();
        for (var criterioDto : dto.criterios()) {
            CriterioRubrica criterio = new CriterioRubrica();
            criterio.setNombre(criterioDto.nombre());
            criterio.setDescripcion(criterioDto.descripcion());
            criterio.setPeso(criterioDto.peso());
            criterio.setRubrica(rubrica);

            List<NivelRubrica> niveles = new ArrayList<>();
            if (criterioDto.niveles() != null) {
                for (var nivelDto : criterioDto.niveles()) {
                    niveles.add(new NivelRubrica(nivelDto.nombre(), nivelDto.puntaje(), nivelDto.descriptor()));
                }
            }
            criterio.setNiveles(niveles);
            criterios.add(criterio);
        }
        rubrica.setCriterios(criterios);

        return rubricaRepositorio.save(rubrica);
    }

    public List<Rubrica> listarPorCurso(String cursoId) {
        return rubricaRepositorio.findByCursoIdOrderByFechaGeneracionDesc(cursoId);
    }

    public Rubrica buscarPorId(String id) throws MyException {
        return rubricaRepositorio.findById(id)
                .orElseThrow(() -> new MyException("Rúbrica no encontrada"));
    }
}
