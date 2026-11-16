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
 * Borra columnas (y tablas) que quedaron de versiones anteriores y pasa los datos viejos al formato nuevo. Con ddl-auto=update Hibernate agrega columnas
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

    // Tablas que ya no se usan: password_reset_codes se reemplazó por verification_codes
    private static final List<String> LEGACY_TABLES = List.of("password_reset_codes");

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
        migrateTextInterests();
        for (String table : LEGACY_TABLES) {
            if (tableExists(table)) {
                jdbcTemplate.execute("drop table " + table);
                log.info("Tabla vieja borrada: {}", table);
            }
        }
    }

    /**
     * Antes los intereses eran texto suelto (experience_tags.tag y user_interests.interest). Ahora son la entidad
     * Interest, relacionada con experiencias (experience_interest) y usuarios (user_interest). Si una base todavía tiene
     * las tablas viejas, se crea un interés por cada etiqueta distinta (en la categoría de una experiencia que la usaba),
     * se copian las relaciones por nombre y se borran las tablas viejas. Un interés de usuario que ninguna experiencia
     * usaba se descarta: no traía ningún plan.
     */
    private void migrateTextInterests() {
        if (tableExists("experience_tags")) {
            jdbcTemplate.update("insert into interests (name, category_id) "
                    + "select t.tag, min(e.category_id) from experience_tags t join experiences e on e.id = t.experience_id "
                    + "where t.tag not in (select name from interests) group by t.tag");
            jdbcTemplate.update("insert ignore into experience_interest (experience_id, interest_id) "
                    + "select t.experience_id, i.id from experience_tags t join interests i on i.name = t.tag");
            jdbcTemplate.execute("drop table experience_tags");
            log.info("Intereses migrados: experience_tags → interests + experience_interest");
        }
        if (tableExists("user_interests")) {
            jdbcTemplate.update("insert ignore into user_interest (user_id, interest_id) "
                    + "select u.user_id, i.id from user_interests u join interests i on i.name = u.interest");
            jdbcTemplate.execute("drop table user_interests");
            log.info("Intereses migrados: user_interests → user_interest");
        }
    }

    private boolean tableExists(String table) {
        Integer found = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = database() and table_name = ?",
                Integer.class, table);
        return found != null && found > 0;
    }

    private boolean exists(String table, String column) {
        Integer found = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns "
                        + "where table_schema = database() and table_name = ? and column_name = ?",
                Integer.class, table, column);
        return found != null && found > 0;
    }
}
