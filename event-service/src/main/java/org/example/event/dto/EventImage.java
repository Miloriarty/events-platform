package org.example.event.dto;

// Содержимое картинки для отдачи клиенту
public record EventImage(String contentType, byte[] data) {}
