package org.example.event.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Entity
@Table(name = "locations")
@Schema(description = "Локация проведения мероприятий")
public class LocationsModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "ID локации", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "Location name is required")
    @Schema(description = "Название локации", example = "Conference Hall A")
    private String name;

    @NotBlank(message = "Address is required")
    @Schema(description = "Адрес", example = "Main Street 15")
    private String address;

    @NotBlank(message = "City is required")
    @Schema(description = "Город", example = "Prague")
    private String city;

    @NotNull(message = "Capacity is required")
    @Positive(message = "Capacity must be greater than 0")
    @Schema(description = "Вместимость", example = "500")
    private Integer capacity;

    public LocationsModel() {}

    public LocationsModel(Long id, String name, String address, String city, Integer capacity) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.city = city;
        this.capacity = capacity;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
}