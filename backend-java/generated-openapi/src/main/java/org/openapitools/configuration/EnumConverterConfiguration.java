package org.openapitools.configuration;

import com.ecargohub.backend.api.model.AlertSeverity;
import com.ecargohub.backend.api.model.AlertType;
import com.ecargohub.backend.api.model.RouteStatus;
import com.ecargohub.backend.api.model.SimulationStatus;
import com.ecargohub.backend.api.model.VehicleCommandType;
import com.ecargohub.backend.api.model.VehicleStatus;
import com.ecargohub.backend.api.model.VehicleType;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;

/**
 * This class provides Spring Converter beans for the enum models in the OpenAPI specification.
 *
 * By default, Spring only converts primitive types to enums using Enum::valueOf, which can prevent
 * correct conversion if the OpenAPI specification is using an `enumPropertyNaming` other than
 * `original` or the specification has an integer enum.
 */
@Configuration(value = "org.openapitools.configuration.enumConverterConfiguration")
public class EnumConverterConfiguration {

    @Bean(name = "org.openapitools.configuration.EnumConverterConfiguration.alertSeverityConverter")
    Converter<String, AlertSeverity> alertSeverityConverter() {
        return new Converter<String, AlertSeverity>() {
            @Override
            public AlertSeverity convert(String source) {
                return AlertSeverity.fromValue(source);
            }
        };
    }
    @Bean(name = "org.openapitools.configuration.EnumConverterConfiguration.alertTypeConverter")
    Converter<String, AlertType> alertTypeConverter() {
        return new Converter<String, AlertType>() {
            @Override
            public AlertType convert(String source) {
                return AlertType.fromValue(source);
            }
        };
    }
    @Bean(name = "org.openapitools.configuration.EnumConverterConfiguration.routeStatusConverter")
    Converter<String, RouteStatus> routeStatusConverter() {
        return new Converter<String, RouteStatus>() {
            @Override
            public RouteStatus convert(String source) {
                return RouteStatus.fromValue(source);
            }
        };
    }
    @Bean(name = "org.openapitools.configuration.EnumConverterConfiguration.simulationStatusConverter")
    Converter<String, SimulationStatus> simulationStatusConverter() {
        return new Converter<String, SimulationStatus>() {
            @Override
            public SimulationStatus convert(String source) {
                return SimulationStatus.fromValue(source);
            }
        };
    }
    @Bean(name = "org.openapitools.configuration.EnumConverterConfiguration.vehicleCommandTypeConverter")
    Converter<String, VehicleCommandType> vehicleCommandTypeConverter() {
        return new Converter<String, VehicleCommandType>() {
            @Override
            public VehicleCommandType convert(String source) {
                return VehicleCommandType.fromValue(source);
            }
        };
    }
    @Bean(name = "org.openapitools.configuration.EnumConverterConfiguration.vehicleStatusConverter")
    Converter<String, VehicleStatus> vehicleStatusConverter() {
        return new Converter<String, VehicleStatus>() {
            @Override
            public VehicleStatus convert(String source) {
                return VehicleStatus.fromValue(source);
            }
        };
    }
    @Bean(name = "org.openapitools.configuration.EnumConverterConfiguration.vehicleTypeConverter")
    Converter<String, VehicleType> vehicleTypeConverter() {
        return new Converter<String, VehicleType>() {
            @Override
            public VehicleType convert(String source) {
                return VehicleType.fromValue(source);
            }
        };
    }

}
