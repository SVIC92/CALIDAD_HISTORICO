package com.GestionInscripcionCursos.configuracion;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SchemaBootstrap implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public SchemaBootstrap(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Ensures 2FA columns exist on legacy databases before entities are used.
        jdbcTemplate.execute("ALTER TABLE usuario ADD COLUMN IF NOT EXISTS two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE");
        jdbcTemplate.execute("ALTER TABLE usuario ADD COLUMN IF NOT EXISTS two_factor_secret VARCHAR(255)");
    }
}
