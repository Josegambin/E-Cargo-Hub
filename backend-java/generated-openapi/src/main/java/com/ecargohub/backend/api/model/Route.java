package com.ecargohub.backend.api.model;

import java.net.URI;
import java.util.Objects;
import com.ecargohub.backend.api.model.GeoLineString;
import com.ecargohub.backend.api.model.GeoPoint;
import com.ecargohub.backend.api.model.RouteStatus;
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
 * Route
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-09-25T06:54:58.902000414Z[Etc/UTC]", comments = "Generator version: 7.15.0")
public class Route {

  private Long id;

  private String name;

  private GeoPoint origin;

  private GeoPoint destination;

  private Double distanceMeters;

  private Long durationSeconds;

  private RouteStatus status;

  private @Nullable GeoLineString geometry;

  public Route() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Route(Long id, String name, GeoPoint origin, GeoPoint destination, Double distanceMeters, Long durationSeconds, RouteStatus status) {
    this.id = id;
    this.name = name;
    this.origin = origin;
    this.destination = destination;
    this.distanceMeters = distanceMeters;
    this.durationSeconds = durationSeconds;
    this.status = status;
  }

  public Route id(Long id) {
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

  public Route name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  @NotNull 
  @Schema(name = "name", example = "Alicante - Madrid", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Route origin(GeoPoint origin) {
    this.origin = origin;
    return this;
  }

  /**
   * Get origin
   * @return origin
   */
  @NotNull @Valid 
  @Schema(name = "origin", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("origin")
  public GeoPoint getOrigin() {
    return origin;
  }

  public void setOrigin(GeoPoint origin) {
    this.origin = origin;
  }

  public Route destination(GeoPoint destination) {
    this.destination = destination;
    return this;
  }

  /**
   * Get destination
   * @return destination
   */
  @NotNull @Valid 
  @Schema(name = "destination", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("destination")
  public GeoPoint getDestination() {
    return destination;
  }

  public void setDestination(GeoPoint destination) {
    this.destination = destination;
  }

  public Route distanceMeters(Double distanceMeters) {
    this.distanceMeters = distanceMeters;
    return this;
  }

  /**
   * Get distanceMeters
   * @return distanceMeters
   */
  @NotNull 
  @Schema(name = "distanceMeters", example = "425000", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("distanceMeters")
  public Double getDistanceMeters() {
    return distanceMeters;
  }

  public void setDistanceMeters(Double distanceMeters) {
    this.distanceMeters = distanceMeters;
  }

  public Route durationSeconds(Long durationSeconds) {
    this.durationSeconds = durationSeconds;
    return this;
  }

  /**
   * Get durationSeconds
   * @return durationSeconds
   */
  @NotNull 
  @Schema(name = "durationSeconds", example = "14400", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("durationSeconds")
  public Long getDurationSeconds() {
    return durationSeconds;
  }

  public void setDurationSeconds(Long durationSeconds) {
    this.durationSeconds = durationSeconds;
  }

  public Route status(RouteStatus status) {
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
  public RouteStatus getStatus() {
    return status;
  }

  public void setStatus(RouteStatus status) {
    this.status = status;
  }

  public Route geometry(@Nullable GeoLineString geometry) {
    this.geometry = geometry;
    return this;
  }

  /**
   * Get geometry
   * @return geometry
   */
  @Valid 
  @Schema(name = "geometry", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("geometry")
  public @Nullable GeoLineString getGeometry() {
    return geometry;
  }

  public void setGeometry(@Nullable GeoLineString geometry) {
    this.geometry = geometry;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Route route = (Route) o;
    return Objects.equals(this.id, route.id) &&
        Objects.equals(this.name, route.name) &&
        Objects.equals(this.origin, route.origin) &&
        Objects.equals(this.destination, route.destination) &&
        Objects.equals(this.distanceMeters, route.distanceMeters) &&
        Objects.equals(this.durationSeconds, route.durationSeconds) &&
        Objects.equals(this.status, route.status) &&
        Objects.equals(this.geometry, route.geometry);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, origin, destination, distanceMeters, durationSeconds, status, geometry);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Route {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    origin: ").append(toIndentedString(origin)).append("\n");
    sb.append("    destination: ").append(toIndentedString(destination)).append("\n");
    sb.append("    distanceMeters: ").append(toIndentedString(distanceMeters)).append("\n");
    sb.append("    durationSeconds: ").append(toIndentedString(durationSeconds)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    geometry: ").append(toIndentedString(geometry)).append("\n");
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

