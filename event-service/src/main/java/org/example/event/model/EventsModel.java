package org.example.event.model;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "events")
public class EventsModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private LocationsModel location;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private CategoriesModel category;

    // id пользователя из user-service: просто число, без FK и без JPA-связи
    @Column(name = "organizer_id", nullable = false)
    private Long organizerId;

    @Column(nullable = false)
    private Integer maxParticipants;

    public EventsModel() {}

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public LocationsModel getLocation() { return location; }
    public void setLocation(LocationsModel location) { this.location = location; }
    public CategoriesModel getCategory() { return category; }
    public void setCategory(CategoriesModel category) { this.category = category; }
    public Long getOrganizerId() { return organizerId; }
    public void setOrganizerId(Long organizerId) { this.organizerId = organizerId; }
    public Integer getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(Integer maxParticipants) { this.maxParticipants = maxParticipants; }
}