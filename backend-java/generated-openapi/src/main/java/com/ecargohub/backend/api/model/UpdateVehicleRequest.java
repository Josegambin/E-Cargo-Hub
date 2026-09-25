package com.ecargohub.backend.api.model;

import java.net.URI;
import java.util.Objects;
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
 * UpdateVehicleRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-09-25T06:54:58.902000414Z[Etc/UTC]", comments = "Generator version: 7.15.0")
public class UpdateVehicleRequest {

  private String name;

  private VehicleType type;

  private Double maxSpeed;

  public UpdateVehicleRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public UpdateVehicleRequest(String name, VehicleType type, Double maxSpeed) {
    this.name = name;
    this.type = type;
    this.maxSpeed = maxSpeed;
  }

  public UpdateVehicleRequest name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  @NotNull @Size(min = 1, max = 100) 
  @Schema(name = "name", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public UpdateVehicleRequest type(VehicleType type) {
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

  public UpdateVehicleRequest maxSpeed(Double maxSpeed) {
    this.maxSpeed = maxSpeed;
    return this;
  }

  /**
   * Get maxSpeed
   * minimum: 0
   * @return maxSpeed
   */
  @NotNull @DecimalMin("0") 
  @Schema(name = "maxSpeed", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("maxSpeed")
  public Double getMaxSpeed() {
    return maxSpeed;
  }

  public void setMaxSpeed(Double maxSpeed) {
    this.maxSpeed = maxSpeed;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UpdateVehicleRequest updateVehicleRequest = (UpdateVehicleRequest) o;
    return Objects.equals(this.name, updateVehicleRequest.name) &&
        Objects.equals(this.type, updateVehicleRequest.type) &&
        Objects.equals(this.maxSpeed, updateVehicleRequest.maxSpeed);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type, maxSpeed);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateVehicleRequest {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    maxSpeed: ").append(toIndentedString(maxSpeed)).append("\n");
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

