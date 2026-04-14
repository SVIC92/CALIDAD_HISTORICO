import { Grid, Card, CardContent, Typography, CardActionArea, Box } from '@mui/material';
import { ListAlt, PersonAdd, Assessment, Settings, AssignmentTurnedIn, School, Book } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';

const submodulosPorRol = {
  ROLE_ADMIN: [
    {
      title: 'Listado de Cursos',
      description: 'Ver, editar y gestionar la oferta académica disponible.',
      icon: <ListAlt sx={{ fontSize: 50, color: 'primary.main' }} />,
      path: '/cursos/listado',
    },
    {
      title: 'Inscripción Manual',
      description: 'Inscribir alumnos en cursos específicos de forma administrativa.',
      icon: <PersonAdd sx={{ fontSize: 50, color: 'secondary.main' }} />,
      path: '/modulo/inscripciones',
    },
    {
      title: 'Reportes por Curso',
      description: 'Generar estadísticas de asistencia y rendimiento por módulo.',
      icon: <Assessment sx={{ fontSize: 50, color: 'success.main' }} />,
      path: '/modulo/reportes',
    },
    {
      title: 'Configuración',
      description: 'Ajustar parámetros generales de matriculación y fechas.',
      icon: <Settings sx={{ fontSize: 50, color: 'warning.main' }} />,
      path: '/modulo/configuracion',
    },
  ],
  ROLE_PROFESOR: [
    {
      title: 'Cursos Disponibles para Dictar',
      description: 'Inscríbete para dictar cursos que todavía no tienen profesor asignado.',
      icon: <School sx={{ fontSize: 50, color: 'primary.main' }} />,
      path: '/cursos/listado',
    },
    {
      title: 'Cursos a Dictar',
      description: 'Visualiza cursos donde tu solicitud fue aprobada como docente.',
      icon: <Book sx={{ fontSize: 50, color: 'secondary.main' }} />,
      path: '/cursos/dictados',
    },
    {
      title: 'Actividades',
      description: 'Crea, edita y publica actividades para tus estudiantes.',
      icon: <ListAlt sx={{ fontSize: 50, color: 'success.main' }} />,
      path: '/modulo/actividades',
    },
    {
      title: 'Inscripciones',
      description: 'Revisa y gestiona solicitudes de inscripción pendientes.',
      icon: <AssignmentTurnedIn sx={{ fontSize: 50, color: 'warning.main' }} />,
      path: '/modulo/inscripciones',
    },
    {
      title: 'Reportes',
      description: 'Consulta reportes de avance y calificaciones por actividad.',
      icon: <Assessment sx={{ fontSize: 50, color: 'warning.main' }} />,
      path: '/modulo/reportes',
    },
  ],
  ROLE_ALUMNO: [
    {
      title: 'Cursos Disponibles',
      description: 'Explora y selecciona cursos para tu periodo académico.',
      icon: <Book sx={{ fontSize: 50, color: 'primary.main' }} />,
      path: '/cursos/listado',
    },
    {
      title: 'Mis Cursos Inscritos',
      description: 'Consulta los cursos en los que ya te encuentras inscrito.',
      icon: <AssignmentTurnedIn sx={{ fontSize: 50, color: 'secondary.main' }} />,
      path: '/modulo/inscripciones',
    },
    {
      title: 'Actividades',
      description: 'Accede a tus actividades y entregas asignadas.',
      icon: <ListAlt sx={{ fontSize: 50, color: 'success.main' }} />,
      path: '/modulo/actividades',
    },
    {
      title: 'Mis Reportes',
      description: 'Visualiza reportes, notas y retroalimentación recibida.',
      icon: <Assessment sx={{ fontSize: 50, color: 'warning.main' }} />,
      path: '/modulo/reportes',
    },
  ],
};

const titulosHub = {
  ROLE_ADMIN: 'Gestión de Cursos - Admin',
  ROLE_PROFESOR: 'Gestión de Cursos - Profesor',
  ROLE_ALUMNO: 'Gestión de Cursos - Alumno',
};

const CursosHub = () => {
  const navigate = useNavigate();
  const rol = localStorage.getItem('rol');
  const submodulos = submodulosPorRol[rol] || submodulosPorRol.ROLE_ALUMNO;
  const titulo = titulosHub[rol] || titulosHub.ROLE_ALUMNO;

  return (
    <Box>
      <Typography variant="h4" sx={{ mb: 1, fontWeight: 'bold' }}>
        {titulo}
      </Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
        Seleccione el sub-módulo con el que desea trabajar hoy.
      </Typography>

      <Grid container spacing={4}>
        {submodulos.map((item) => (
          <Grid size={{ xs: 12, sm: 6, md: 3 }} key={item.title}>
            <Card sx={{ height: '100%', textAlign: 'center', transition: '0.3s', '&:hover': { transform: 'translateY(-5px)', boxShadow: 6 } }}>
              <CardActionArea sx={{ height: '100%', p: 3 }} onClick={() => navigate(item.path)}>
                <Box sx={{ mb: 2 }}>{item.icon}</Box>
                <CardContent>
                  <Typography gutterBottom variant="h6" component="div" sx={{ fontWeight: 'bold' }}>
                    {item.title}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {item.description}
                  </Typography>
                </CardContent>
              </CardActionArea>
            </Card>
          </Grid>
        ))}
      </Grid>
    </Box>
  );
};

export default CursosHub;