package com.ecargohub.backend.api.model;

import java.net.URI;
import java.util.Objects;
import com.ecargohub.backend.api.model.VehicleCommandType;
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
 * VehicleCommandRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-09-25T06:54:58.902000414Z[Etc/UTC]", comments = "Generator version: 7.15.0")
public class VehicleCommandRequest {

  private VehicleCommandType command;

  private @Nullable Double value;

  public VehicleCommandRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public VehicleCommandRequest(VehicleCommandType command) {
    this.command = command;
  }

  public VehicleCommandRequest command(VehicleCommandType command) {
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

  public VehicleCommandRequest value(@Nullable Double value) {
    this.value = value;
    return this;
  }

  /**
   * Valor asociado al comando. Por ejemplo, velocidad objetivo para SET_SPEED. 
   * @return value
   */
  
  @Schema(name = "value", example = "80", description = "Valor asociado al comando. Por ejemplo, velocidad objetivo para SET_SPEED. ", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("value")
  public @Nullable Double getValue() {
    return value;
  }

  public void setValue(@Nullable Double value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    VehicleCommandRequest vehicleCommandRequest = (VehicleCommandRequest) o;
    return Objects.equals(this.command, vehicleCommandRequest.command) &&
        Objects.equals(this.value, vehicleCommandRequest.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(command, value);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class VehicleCommandRequest {\n");
    sb.append("    command: ").append(toIndentedString(command)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
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

