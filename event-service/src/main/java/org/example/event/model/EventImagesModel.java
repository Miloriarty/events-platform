package org.example.event.model;

import jakarta.persistence.*;

// Картинка мероприятия в отдельной таблице: списки мероприятий не загружают байты.
// Первичный ключ совпадает с id мероприятия (одна картинка на мероприятие)
@Entity
@Table(name = "event_images")
public class EventImagesModel {

    @Id
    @Column(name = "event_id")
    private Long eventId;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private byte[] data;

    public EventImagesModel() {}

    public EventImagesModel(Long eventId, String contentType, byte[] data) {
        this.eventId = eventId;
        this.contentType = contentType;
        this.data = data;
    }

    public Long getEventId() { return eventId; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }
}
