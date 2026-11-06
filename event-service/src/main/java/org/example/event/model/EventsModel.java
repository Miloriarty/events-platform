package org.example.event.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
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

    // DEFAULT нужен ddl-auto: update, иначе NOT NULL-колонка не добавится в таблицу с данными.
    // У старых мероприятий будет 0, то есть подходит любой билет
    @Column(name = "min_ticket_price", columnDefinition = "numeric(12,2) default 0 not null")
    private BigDecimal minTicketPrice = BigDecimal.ZERO;

    // время последней загрузки картинки (мс); null - картинки нет.
    // Входит в URL картинки, чтобы браузер не показывал старую из кеша
    @Column(name = "image_version")
    private Long imageVersion;

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
    public BigDecimal getMinTicketPrice() { return minTicketPrice; }
    public void setMinTicketPrice(BigDecimal minTicketPrice) { this.minTicketPrice = minTicketPrice; }
    public Long getImageVersion() { return imageVersion; }
    public void setImageVersion(Long imageVersion) { this.imageVersion = imageVersion; }
}