package org.example.notification.service;

import jakarta.annotation.PreDestroy;
import org.example.notification.dto.NotificationResponse;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

// Открытые SSE-соединения. У пользователя их может быть несколько: по одному на вкладку.
// Они живут в памяти процесса, поэтому сервис рассчитан на один экземпляр
@Component
public class NotificationStream {

    // По истечении клиент переподключается и заново проходит проверку токена в gateway
    private static final long TIMEOUT_MS = Duration.ofMinutes(30).toMillis();

    private final Map<Long, List<SseEmitter>> clients = new ConcurrentHashMap<>();

    public SseEmitter connect(Long userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        clients.compute(userId, (id, emitters) -> {
            List<SseEmitter> list = emitters != null ? emitters : new CopyOnWriteArrayList<>();
            list.add(emitter);
            return list;
        });
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onError(e -> remove(userId, emitter));
        emitter.onTimeout(emitter::complete);
        // первый фрагмент сразу отправляет заголовки ответа, иначе клиент ждет до первого события
        send(userId, emitter, SseEmitter.event().comment("connected"));
        return emitter;
    }

    public void send(Long userId, NotificationResponse notification) {
        clients.getOrDefault(userId, List.of()).forEach(emitter -> send(userId, emitter,
                SseEmitter.event().name("notification").data(notification, MediaType.APPLICATION_JSON)));
    }

    // без трафика соединение может закрыть прокси, а закрытое клиентом обнаруживается только при записи
    @Scheduled(fixedDelay = 25_000)
    void ping() {
        clients.forEach((userId, emitters) -> emitters.forEach(
                emitter -> send(userId, emitter, SseEmitter.event().comment("ping"))));
    }

    // иначе при остановке Tomcat ждет завершения открытых запросов
    @PreDestroy
    void closeAll() {
        clients.values().forEach(emitters -> emitters.forEach(SseEmitter::complete));
    }

    private void send(Long userId, SseEmitter emitter, SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException e) {
            remove(userId, emitter);       // вкладку закрыли
        }
    }

    private void remove(Long userId, SseEmitter emitter) {
        clients.computeIfPresent(userId, (id, emitters) -> {
            emitters.remove(emitter);
            return emitters.isEmpty() ? null : emitters;
        });
    }
}
