package com.ecargohub.backend.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
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
 * VehiclePosition
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-09-25T06:54:58.902000414Z[Etc/UTC]", comments = "Generator version: 7.15.0")
public class VehiclePosition {

  private Long vehicleId;

  private Double latitude;

  private Double longitude;

  private Double speed;

  private @Nullable Double speedLimit;

  private @Nullable Double acceleration;

  private Double heading;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime timestamp;

  public VehiclePosition() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public VehiclePosition(Long vehicleId, Double latitude, Double longitude, Double speed, Double heading, OffsetDateTime timestamp) {
    this.vehicleId = vehicleId;
    this.latitude = latitude;
    this.longitude = longitude;
    this.speed = speed;
    this.heading = heading;
    this.timestamp = timestamp;
  }

  public VehiclePosition vehicleId(Long vehicleId) {
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

  public VehiclePosition latitude(Double latitude) {
    this.latitude = latitude;
    return this;
  }

  /**
   * Get latitude
   * minimum: -90
   * maximum: 90
   * @return latitude
   */
  @NotNull @DecimalMin("-90") @DecimalMax("90") 
  @Schema(name = "latitude", example = "38.3452", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("latitude")
  public Double getLatitude() {
    return latitude;
  }

  public void setLatitude(Double latitude) {
    this.latitude = latitude;
  }

  public VehiclePosition longitude(Double longitude) {
    this.longitude = longitude;
    return this;
  }

  /**
   * Get longitude
   * minimum: -180
   * maximum: 180
   * @return longitude
   */
  @NotNull @DecimalMin("-180") @DecimalMax("180") 
  @Schema(name = "longitude", example = "-0.481", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("longitude")
  public Double getLongitude() {
    return longitude;
  }

  public void setLongitude(Double longitude) {
    this.longitude = longitude;
  }

  public VehiclePosition speed(Double speed) {
    this.speed = speed;
    return this;
  }

  /**
   * Get speed
   * minimum: 0
   * @return speed
   */
  @NotNull @DecimalMin("0") 
  @Schema(name = "speed", example = "92.5", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("speed")
  public Double getSpeed() {
    return speed;
  }

  public void setSpeed(Double speed) {
    this.speed = speed;
  }

  public VehiclePosition speedLimit(@Nullable Double speedLimit) {
    this.speedLimit = speedLimit;
    return this;
  }

  /**
   * Get speedLimit
   * minimum: 0
   * @return speedLimit
   */
  @DecimalMin("0") 
  @Schema(name = "speedLimit", example = "90", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("speedLimit")
  public @Nullable Double getSpeedLimit() {
    return speedLimit;
  }

  public void setSpeedLimit(@Nullable Double speedLimit) {
    this.speedLimit = speedLimit;
  }

  public VehiclePosition acceleration(@Nullable Double acceleration) {
    this.acceleration = acceleration;
    return this;
  }

  /**
   * Get acceleration
   * @return acceleration
   */
  
  @Schema(name = "acceleration", example = "0.8", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("acceleration")
  public @Nullable Double getAcceleration() {
    return acceleration;
  }

  public void setAcceleration(@Nullable Double acceleration) {
    this.acceleration = acceleration;
  }

  public VehiclePosition heading(Double heading) {
    this.heading = heading;
    return this;
  }

  /**
   * Get heading
   * minimum: 0
   * maximum: 360
   * @return heading
   */
  @NotNull @DecimalMin("0") @DecimalMax("360") 
  @Schema(name = "heading", example = "274.2", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("heading")
  public Double getHeading() {
    return heading;
  }

  public void setHeading(Double heading) {
    this.heading = heading;
  }

  public VehiclePosition timestamp(OffsetDateTime timestamp) {
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
    VehiclePosition vehiclePosition = (VehiclePosition) o;
    return Objects.equals(this.vehicleId, vehiclePosition.vehicleId) &&
        Objects.equals(this.latitude, vehiclePosition.latitude) &&
        Objects.equals(this.longitude, vehiclePosition.longitude) &&
        Objects.equals(this.speed, vehiclePosition.speed) &&
        Objects.equals(this.speedLimit, vehiclePosition.speedLimit) &&
        Objects.equals(this.acceleration, vehiclePosition.acceleration) &&
        Objects.equals(this.heading, vehiclePosition.heading) &&
        Objects.equals(this.timestamp, vehiclePosition.timestamp);
  }

  @Override
  public int hashCode() {
    return Objects.hash(vehicleId, latitude, longitude, speed, speedLimit, acceleration, heading, timestamp);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class VehiclePosition {\n");
    sb.append("    vehicleId: ").append(toIndentedString(vehicleId)).append("\n");
    sb.append("    latitude: ").append(toIndentedString(latitude)).append("\n");
    sb.append("    longitude: ").append(toIndentedString(longitude)).append("\n");
    sb.append("    speed: ").append(toIndentedString(speed)).append("\n");
    sb.append("    speedLimit: ").append(toIndentedString(speedLimit)).append("\n");
    sb.append("    acceleration: ").append(toIndentedString(acceleration)).append("\n");
    sb.append("    heading: ").append(toIndentedString(heading)).append("\n");
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

