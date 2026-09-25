package com.ecargohub.backend.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GeoLineStringEmbeddable {

    @Column(name = "geometry_type")
    private String type; // "LineString"

    @Convert(converter = CoordinatesConverter.class)
    @Column(name = "coordinates", columnDefinition = "TEXT")
    private List<List<Double>> coordinates;

    @Converter
    public static class CoordinatesConverter
            implements AttributeConverter<List<List<Double>>, String> {

        private static final ObjectMapper MAPPER = new ObjectMapper();

        @Override
        public String convertToDatabaseColumn(List<List<Double>> attribute) {
            try {
                return attribute == null ? null : MAPPER.writeValueAsString(attribute);
            } catch (Exception e) {
                throw new IllegalStateException("Error serializando coordinates", e);
            }
        }

        @Override
        public List<List<Double>> convertToEntityAttribute(String dbData) {
            try {
                return dbData == null
                        ? null
                        : MAPPER.readValue(dbData, new TypeReference<>() {});
            } catch (Exception e) {
                throw new IllegalStateException("Error deserializando coordinates", e);
            }
        }
    }
}