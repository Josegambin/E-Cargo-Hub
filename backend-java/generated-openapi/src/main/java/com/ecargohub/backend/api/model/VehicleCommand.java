package com.ecargohub.backend.api.model;

import java.net.URI;
import java.util.Objects;
import com.ecargohub.backend.api.model.VehicleCommandType;
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
 * VehicleCommand
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-09-25T06:54:58.902000414Z[Etc/UTC]", comments = "Generator version: 7.15.0")
public class VehicleCommand {

  private Long id;

  private Long vehicleId;

  private VehicleCommandType command;

  private @Nullable Double value;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  public VehicleCommand() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public VehicleCommand(Long id, Long vehicleId, VehicleCommandType command, OffsetDateTime createdAt) {
    this.id = id;
    this.vehicleId = vehicleId;
    this.command = command;
    this.createdAt = createdAt;
  }

  public VehicleCommand id(Long id) {
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

  public VehicleCommand vehicleId(Long vehicleId) {
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

  public VehicleCommand command(VehicleCommandType command) {
    this.command = command;
    return this;
  }

  /**
   * Get command
   * @return command
   */
  @NotNull @Valid 
  @Schema(name = "command", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("command")
  public VehicleCommandType getCommand() {
    return command;
  }

  public void setCommand(VehicleCommandType command) {
    this.command = command;
  }

  public VehicleCommand value(@Nullable Double value) {
    this.value = value;
    return this;
  }

  /**
   * Get value
   * @return value
   */
  
  @Schema(name = "value", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("value")
  public @Nullable Double getValue() {
    return value;
  }

  public void setValue(@Nullable Double value) {
    this.value = value;
  }

  public VehicleCommand createdAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  /**
   * Get createdAt
   * @return createdAt
   */
  @NotNull @Valid 
  @Schema(name = "createdAt", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("createdAt")
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    VehicleCommand vehicleCommand = (VehicleCommand) o;
    return Objects.equals(this.id, vehicleCommand.id) &&
        Objects.equals(this.vehicleId, vehicleCommand.vehicleId) &&
        Objects.equals(this.command, vehicleCommand.command) &&
        Objects.equals(this.value, vehicleCommand.value) &&
        Objects.equals(this.createdAt, vehicleCommand.createdAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, vehicleId, command, value, createdAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class VehicleCommand {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    vehicleId: ").append(toIndentedString(vehicleId)).append("\n");
    sb.append("    command: ").append(toIndentedString(command)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
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

