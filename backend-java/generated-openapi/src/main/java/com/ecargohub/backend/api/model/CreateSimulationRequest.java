package com.ecargohub.backend.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
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
 * CreateSimulationRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-09-25T06:54:58.902000414Z[Etc/UTC]", comments = "Generator version: 7.15.0")
public class CreateSimulationRequest {

  private String name;

  private Long vehicleId;

  private Long routeId;

  private Double simulationSpeed = 1d;

  public CreateSimulationRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CreateSimulationRequest(String name, Long vehicleId, Long routeId) {
    this.name = name;
    this.vehicleId = vehicleId;
    this.routeId = routeId;
  }

  public CreateSimulationRequest name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  @NotNull @Size(min = 1, max = 150) 
  @Schema(name = "name", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public CreateSimulationRequest vehicleId(Long vehicleId) {
    this.vehicleId = vehicleId;
    return this;
  }

  /**
   * Get vehicleId
   * @return vehicleId
   */
  @NotNull 
  @Schema(name = "vehicleId", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("vehicleId")
  public Long getVehicleId() {
    return vehicleId;
  }

  public void setVehicleId(Long vehicleId) {
    this.vehicleId = vehicleId;
  }

  public CreateSimulationRequest routeId(Long routeId) {
    this.routeId = routeId;
    return this;
  }

  /**
   * Get routeId
   * @return routeId
   */
  @NotNull 
  @Schema(name = "routeId", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("routeId")
  public Long getRouteId() {
    return routeId;
  }

  public void setRouteId(Long routeId) {
    this.routeId = routeId;
  }

  public CreateSimulationRequest simulationSpeed(Double simulationSpeed) {
    this.simulationSpeed = simulationSpeed;
    return this;
  }

  /**
   * Get simulationSpeed
   * minimum: 0
   * @return simulationSpeed
   */
  @DecimalMin("0") 
  @Schema(name = "simulationSpeed", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("simulationSpeed")
  public Double getSimulationSpeed() {
    return simulationSpeed;
  }

  public void setSimulationSpeed(Double simulationSpeed) {
    this.simulationSpeed = simulationSpeed;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateSimulationRequest createSimulationRequest = (CreateSimulationRequest) o;
    return Objects.equals(this.name, createSimulationRequest.name) &&
        Objects.equals(this.vehicleId, createSimulationRequest.vehicleId) &&
        Objects.equals(this.routeId, createSimulationRequest.routeId) &&
        Objects.equals(this.simulationSpeed, createSimulationRequest.simulationSpeed);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, vehicleId, routeId, simulationSpeed);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateSimulationRequest {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    vehicleId: ").append(toIndentedString(vehicleId)).append("\n");
    sb.append("    routeId: ").append(toIndentedString(routeId)).append("\n");
    sb.append("    simulationSpeed: ").append(toIndentedString(simulationSpeed)).append("\n");
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

