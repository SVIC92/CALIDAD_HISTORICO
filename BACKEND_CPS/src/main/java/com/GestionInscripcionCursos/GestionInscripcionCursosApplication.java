package com.GestionInscripcionCursos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GestionInscripcionCursosApplication {

	public static void main(String[] args) {
		SpringApplication.run(GestionInscripcionCursosApplication.class, args);
	}

}
