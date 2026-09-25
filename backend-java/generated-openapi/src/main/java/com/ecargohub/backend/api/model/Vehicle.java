package com.ecargohub.backend.api.model;

import java.net.URI;
import java.util.Objects;
import com.ecargohub.backend.api.model.VehiclePosition;
import com.ecargohub.backend.api.model.VehicleStatus;
import com.ecargohub.backend.api.model.VehicleType;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Vehicle
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-09-25T06:54:58.902000414Z[Etc/UTC]", comments = "Generator version: 7.15.0")
public class Vehicle {

  private Long id;

  private String name;

  private VehicleType type;

  private Double maxSpeed;

  private VehicleStatus status;

  private @Nullable VehiclePosition currentPosition;

  public Vehicle() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Vehicle(Long id, String name, VehicleType type, Double maxSpeed, VehicleStatus status) {
    this.id = id;
    this.name = name;
    this.type = type;
    this.maxSpeed = maxSpeed;
    this.status = status;
  }

  public Vehicle id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
   */
  @NotNull 
  @Schema(name = "id", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Vehicle name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  @NotNull 
  @Schema(name = "name", example = "TRUCK-001", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Vehicle type(VehicleType type) {
    this.type = type;
    return this;
  }

  /**
   * Get type
   * @return type
   */
  @NotNull @Valid 
  @Schema(name = "type", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("type")
  public VehicleType getType() {
    return type;
  }

  public void setType(VehicleType type) {
    this.type = type;
  }

  public Vehicle maxSpeed(Double maxSpeed) {
    this.maxSpeed = maxSpeed;
    return this;
  }

  /**
   * Get maxSpeed
   * minimum: 0
   * @return maxSpeed
   */
  @NotNull @DecimalMin("0") 
  @Schema(name = "maxSpeed", example = "120", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("maxSpeed")
  public Double getMaxSpeed() {
    return maxSpeed;
  }

  public void setMaxSpeed(Double maxSpeed) {
    this.maxSpeed = maxSpeed;
  }

  public Vehicle status(VehicleStatus status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @NotNull @Valid 
  @Schema(name = "status", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("status")
  public VehicleStatus getStatus() {
    return status;
  }

  public void setStatus(VehicleStatus status) {
    this.status = status;
  }

  public Vehicle currentPosition(@Nullable VehiclePosition currentPosition) {
    this.currentPosition = currentPosition;
    return this;
  }

  /**
   * Get currentPosition
   * @return currentPosition
   */
  @Valid 
  @Schema(name = "currentPosition", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("currentPosition")
  public @Nullable VehiclePosition getCurrentPosition() {
    return currentPosition;
  }

  public void setCurrentPosition(@Nullable VehiclePosition currentPosition) {
    this.currentPosition = currentPosition;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Vehicle vehicle = (Vehicle) o;
    return Objects.equals(this.id, vehicle.id) &&
        Objects.equals(this.name, vehicle.name) &&
        Objects.equals(this.type, vehicle.type) &&
        Objects.equals(this.maxSpeed, vehicle.maxSpeed) &&
        Objects.equals(this.status, vehicle.status) &&
        Objects.equals(this.currentPosition, vehicle.currentPosition);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, type, maxSpeed, status, currentPosition);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Vehicle {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    maxSpeed: ").append(toIndentedString(maxSpeed)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    currentPosition: ").append(toIndentedString(currentPosition)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

