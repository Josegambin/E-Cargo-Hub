package com.ecargohub.backend.api.model;

import java.net.URI;
import java.util.Objects;
import com.ecargohub.backend.api.model.AlertSeverity;
import com.ecargohub.backend.api.model.AlertType;
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
 * VehicleAlert
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-09-25T06:54:58.902000414Z[Etc/UTC]", comments = "Generator version: 7.15.0")
public class VehicleAlert {

  private Long id;

  private Long vehicleId;

  private @Nullable Long simulationId;

  private AlertType type;

  private AlertSeverity severity;

  private String message;

  private @Nullable Double currentSpeed;

  private @Nullable Double speedLimit;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime timestamp;

  public VehicleAlert() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public VehicleAlert(Long id, Long vehicleId, AlertType type, AlertSeverity severity, String message, OffsetDateTime timestamp) {
    this.id = id;
    this.vehicleId = vehicleId;
    this.type = type;
    this.severity = severity;
    this.message = message;
    this.timestamp = timestamp;
  }

  public VehicleAlert id(Long id) {
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

  public VehicleAlert vehicleId(Long vehicleId) {
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

  public VehicleAlert simulationId(@Nullable Long simulationId) {
    this.simulationId = simulationId;
    return this;
  }

  /**
   * Get simulationId
   * @return simulationId
   */
  
  @Schema(name = "simulationId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("simulationId")
  public @Nullable Long getSimulationId() {
    return simulationId;
  }

  public void setSimulationId(@Nullable Long simulationId) {
    this.simulationId = simulationId;
  }

  public VehicleAlert type(AlertType type) {
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
  public AlertType getType() {
    return type;
  }

  public void setType(AlertType type) {
    this.type = type;
  }

  public VehicleAlert severity(AlertSeverity severity) {
    this.severity = severity;
    return this;
  }

  /**
   * Get severity
   * @return severity
   */
  @NotNull @Valid 
  @Schema(name = "severity", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("severity")
  public AlertSeverity getSeverity() {
    return severity;
  }

  public void setSeverity(AlertSeverity severity) {
    this.severity = severity;
  }

  public VehicleAlert message(String message) {
    this.message = message;
    return this;
  }

  /**
   * Get message
   * @return message
   */
  @NotNull 
  @Schema(name = "message", example = "El vehículo supera el límite de velocidad.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("message")
  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public VehicleAlert currentSpeed(@Nullable Double currentSpeed) {
    this.currentSpeed = currentSpeed;
    return this;
  }

  /**
   * Get currentSpeed
   * @return currentSpeed
   */
  
  @Schema(name = "currentSpeed", example = "92", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("currentSpeed")
  public @Nullable Double getCurrentSpeed() {
    return currentSpeed;
  }

  public void setCurrentSpeed(@Nullable Double currentSpeed) {
    this.currentSpeed = currentSpeed;
  }

  public VehicleAlert speedLimit(@Nullable Double speedLimit) {
    this.speedLimit = speedLimit;
    return this;
  }

  /**
   * Get speedLimit
   * @return speedLimit
   */
  
  @Schema(name = "speedLimit", example = "80", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("speedLimit")
  public @Nullable Double getSpeedLimit() {
    return speedLimit;
  }

  public void setSpeedLimit(@Nullable Double speedLimit) {
    this.speedLimit = speedLimit;
  }

  public VehicleAlert timestamp(OffsetDateTime timestamp) {
    this.timestamp = timestamp;
    return this;
  }

  /**
   * Get timestamp
   * @return timestamp
   */
  @NotNull @Valid 
  @Schema(name = "timestamp", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("timestamp")
  public OffsetDateTime getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(OffsetDateTime timestamp) {
    this.timestamp = timestamp;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    VehicleAlert vehicleAlert = (VehicleAlert) o;
    return Objects.equals(this.id, vehicleAlert.id) &&
        Objects.equals(this.vehicleId, vehicleAlert.vehicleId) &&
        Objects.equals(this.simulationId, vehicleAlert.simulationId) &&
        Objects.equals(this.type, vehicleAlert.type) &&
        Objects.equals(this.severity, vehicleAlert.severity) &&
        Objects.equals(this.message, vehicleAlert.message) &&
        Objects.equals(this.currentSpeed, vehicleAlert.currentSpeed) &&
        Objects.equals(this.speedLimit, vehicleAlert.speedLimit) &&
        Objects.equals(this.timestamp, vehicleAlert.timestamp);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, vehicleId, simulationId, type, severity, message, currentSpeed, speedLimit, timestamp);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class VehicleAlert {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    vehicleId: ").append(toIndentedString(vehicleId)).append("\n");
    sb.append("    simulationId: ").append(toIndentedString(simulationId)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    severity: ").append(toIndentedString(severity)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
    sb.append("    currentSpeed: ").append(toIndentedString(currentSpeed)).append("\n");
    sb.append("    speedLimit: ").append(toIndentedString(speedLimit)).append("\n");
    sb.append("    timestamp: ").append(toIndentedString(timestamp)).append("\n");
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

