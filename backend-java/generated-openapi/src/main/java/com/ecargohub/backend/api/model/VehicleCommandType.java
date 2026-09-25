package com.ecargohub.backend.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonValue;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Gets or Sets VehicleCommandType
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-09-25T06:54:58.902000414Z[Etc/UTC]", comments = "Generator version: 7.15.0")
public enum VehicleCommandType {
  
  START("START"),
  
  PAUSE("PAUSE"),
  
  RESUME("RESUME"),
  
  STOP("STOP"),
  
  SET_SPEED("SET_SPEED"),
  
  INCREASE_SPEED("INCREASE_SPEED"),
  
  DECREASE_SPEED("DECREASE_SPEED"),
  
  EMERGENCY_BRAKE("EMERGENCY_BRAKE");

  private final String value;

  VehicleCommandType(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static VehicleCommandType fromValue(String value) {
    for (VehicleCommandType b : VehicleCommandType.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

