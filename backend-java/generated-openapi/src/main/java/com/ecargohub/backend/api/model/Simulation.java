package com.ecargohub.backend.api.model;

import java.net.URI;
import java.util.Objects;
import com.ecargohub.backend.api.model.SimulationStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
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
 * Simulation
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-09-25T06:54:58.902000414Z[Etc/UTC]", comments = "Generator version: 7.15.0")
public class Simulation {

  private Long id;

  private String name;

  private Long vehicleId;

  private Long routeId;

  private SimulationStatus status;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime startedAt;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime finishedAt;

  private @Nullable Double simulationSpeed;

  public Simulation() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Simulation(Long id, String name, Long vehicleId, Long routeId, SimulationStatus status) {
    this.id = id;
    this.name = name;
    this.vehicleId = vehicleId;
    this.routeId = routeId;
    this.status = status;
  }

  public Simulation id(Long id) {
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

  public Simulation name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  @NotNull 
  @Schema(name = "name", example = "Simulación Alicante-Madrid", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Simulation vehicleId(Long vehicleId) {
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

  public Simulation routeId(Long routeId) {
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

  public Simulation status(SimulationStatus status) {
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
  public SimulationStatus getStatus() {
    return status;
  }

  public void setStatus(SimulationStatus status) {
    this.status = status;
  }

  public Simulation startedAt(@Nullable OffsetDateTime startedAt) {
    this.startedAt = startedAt;
    return this;
  }

  /**
   * Get startedAt
   * @return startedAt
   */
  @Valid 
  @Schema(name = "startedAt", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("startedAt")
  public @Nullable OffsetDateTime getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(@Nullable OffsetDateTime startedAt) {
    this.startedAt = startedAt;
  }

  public Simulation finishedAt(@Nullable OffsetDateTime finishedAt) {
    this.finishedAt = finishedAt;
    return this;
  }

  /**
   * Get finishedAt
   * @return finishedAt
   */
  @Valid 
  @Schema(name = "finishedAt", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("finishedAt")
  public @Nullable OffsetDateTime getFinishedAt() {
    return finishedAt;
  }

  public void setFinishedAt(@Nullable OffsetDateTime finishedAt) {
    this.finishedAt = finishedAt;
  }

  public Simulation simulationSpeed(@Nullable Double simulationSpeed) {
    this.simulationSpeed = simulationSpeed;
    return this;
  }

  /**
   * Get simulationSpeed
   * minimum: 0
   * @return simulationSpeed
   */
  @DecimalMin("0") 
  @Schema(name = "simulationSpeed", example = "1", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("simulationSpeed")
  public @Nullable Double getSimulationSpeed() {
    return simulationSpeed;
  }

  public void setSimulationSpeed(@Nullable Double simulationSpeed) {
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
    Simulation simulation = (Simulation) o;
    return Objects.equals(this.id, simulation.id) &&
        Objects.equals(this.name, simulation.name) &&
        Objects.equals(this.vehicleId, simulation.vehicleId) &&
        Objects.equals(this.routeId, simulation.routeId) &&
        Objects.equals(this.status, simulation.status) &&
        Objects.equals(this.startedAt, simulation.startedAt) &&
        Objects.equals(this.finishedAt, simulation.finishedAt) &&
        Objects.equals(this.simulationSpeed, simulation.simulationSpeed);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, vehicleId, routeId, status, startedAt, finishedAt, simulationSpeed);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Simulation {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    vehicleId: ").append(toIndentedString(vehicleId)).append("\n");
    sb.append("    routeId: ").append(toIndentedString(routeId)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    startedAt: ").append(toIndentedString(startedAt)).append("\n");
    sb.append("    finishedAt: ").append(toIndentedString(finishedAt)).append("\n");
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

