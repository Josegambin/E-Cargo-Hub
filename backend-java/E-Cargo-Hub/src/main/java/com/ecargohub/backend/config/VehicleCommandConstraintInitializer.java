package com.ecargohub.backend.config;

import com.ecargohub.backend.domain.enums.VehicleCommandTypeEnum;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class VehicleCommandConstraintInitializer implements ApplicationRunner {

    private static final String CONSTRAINT_NAME = "vehicle_commands_command_check";

    private final JdbcTemplate jdbcTemplate;

    public VehicleCommandConstraintInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        String definition = jdbcTemplate.query(
                "SELECT pg_get_constraintdef(oid) FROM pg_constraint "
                        + "WHERE conrelid = 'vehicle_commands'::regclass AND conname = ?",
                resultSet -> resultSet.next() ? resultSet.getString(1) : null, CONSTRAINT_NAME);
        if (supportsAllCommands(definition)) {
            return;
        }

        String commands = Arrays.stream(VehicleCommandTypeEnum.values()).map(command -> "'" + command.name() + "'")
                .collect(Collectors.joining(", "));
        jdbcTemplate.execute("ALTER TABLE vehicle_commands DROP CONSTRAINT IF EXISTS " + CONSTRAINT_NAME);
        jdbcTemplate.execute("ALTER TABLE vehicle_commands ADD CONSTRAINT " + CONSTRAINT_NAME + " CHECK (command IN ("
                + commands + "))");
    }

    private boolean supportsAllCommands(String definition) {
        if (definition == null) {
            return false;
        }
        return Arrays.stream(VehicleCommandTypeEnum.values())
                .allMatch(command -> definition.contains("'" + command.name() + "'"));
    }
}
