# CALIDAD_HISTORICO
# CALIDAD_HISTORICO - Gestión de Inscripción de Cursos

Repositorio histórico de calidad para el sistema de **Gestión de Inscripción de Cursos**. Este proyecto es una aplicación Full-Stack diseñada para administrar cursos, usuarios e inscripciones, implementando prácticas modernas de desarrollo.

## 🚀 Tecnologías y Stack

El proyecto está dividido en dos partes principales: un Backend robusto en Java y un Frontend dinámico en React.

### Backend (Spring Boot)
- **Lenguaje:** Java 17
- **Framework:** Spring Boot 3.4.0
- **Seguridad:** Spring Security con JSON Web Tokens (JJWT)
- **Persistencia:** Spring Data JPA
- **Base de Datos soportada:** PostgreSQL
- **Gestor de dependencias:** Maven

### Frontend (React)
- **Librería principal:** React 19
- **Entorno de construcción:** Vite
- **Diseño y Componentes:** Material-UI (MUI v9)
- **Gestión de Estado:** Zustand y TanStack React Query
- **Enrutamiento:** React Router DOM v7
- **Peticiones HTTP:** Axios
- **Formularios:** React Hook Form

---

## 📋 Requisitos Previos

Asegúrate de tener instalado lo siguiente en tu entorno local antes de ejecutar el proyecto:

- [Java Development Kit (JDK) 17](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
- [Node.js](https://nodejs.org/) (Versión 18 o superior) y npm/yarn
- [Maven](https://maven.apache.org/)
- Servidor de base de datos PostgreSQL.

---

## ⚙️ Instalación y Ejecución

### 1. Configuración del Backend

1. Navega a la carpeta del backend:
   ```bash
   cd BACKEND_CPS
  Ejecute el siguiente comando:
   mvn spring-boot:run
2. Configuración del Frontend
Abre una nueva terminal y navega a la carpeta del frontend:
  cd FRONTEND_CPS
Instala las dependencias de Node:
  npm install
Inicia el servidor de desarrollo de Vite:
  npm start
