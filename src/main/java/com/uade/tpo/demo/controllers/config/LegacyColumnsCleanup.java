package com.uade.tpo.demo.controllers.config;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Borra columnas que quedaron de versiones anteriores. Con ddl-auto=update Hibernate agrega columnas
 * pero nunca las borra, y algunas eran NOT NULL sin valor por defecto: si quedan, los INSERT fallan.
 * Corre al arrancar, antes que la carga de datos (DataSeeder), y solo actúa si la columna todavía existe.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LegacyColumnsCleanup implements ApplicationRunner {

    // tabla.columna: el estado "terminada", la pausa automática y un monto que ya no se usa
    private static final List<String> LEGACY_COLUMNS = List.of(
            "experience_sessions.finished",
            "experiences.auto_paused",
            "bookings.paid_amount");

    private final JdbcTemplate jdbcTemplate;

    public LegacyColumnsCleanup(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Las que había pausado la tarea automática vuelven a estar publicadas: ahora el catálogo las oculta
        // solo mientras no tengan fechas, y aparecen apenas el anfitrión les agrega una
        if (exists("experiences", "auto_paused")) {
            jdbcTemplate.update("update experiences set active = true where auto_paused = true");
        }
        for (String legacy : LEGACY_COLUMNS) {
            String table = legacy.substring(0, legacy.indexOf('.'));
            String column = legacy.substring(legacy.indexOf('.') + 1);
            if (exists(table, column)) {
                jdbcTemplate.execute("alter table " + table + " drop column " + column);
                log.info("Columna vieja borrada: {}", legacy);
            }
        }
    }

    private boolean exists(String table, String column) {
        Integer found = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns "
                        + "where table_schema = database() and table_name = ? and column_name = ?",
                Integer.class, table, column);
        return found != null && found > 0;
    }
}
