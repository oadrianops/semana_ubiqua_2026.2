package br.com.nandesk.notification.application;

import br.com.nandesk.shared.config.AppProperties;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Conexões SSE abertas por usuário (9.8): o app aberto recebe o contador de não lidas em tempo real; heartbeat a
 * cada 25 s. Com mais de uma réplica, trocar por um barramento compartilhado (RNF-14).
 */
@Component
public class SseHub {

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final Duration timeout;

    public SseHub(AppProperties properties) {
        this.timeout = Duration.ofHours(1);
    }

    public SseEmitter open(Long userId) {
        SseEmitter emitter = new SseEmitter(timeout.toMillis());
        List<SseEmitter> list = emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());
        list.add(emitter);
        Runnable remove = () -> list.remove(emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());
        try {
            emitter.send(SseEmitter.event().name("ready").data(Map.of("ok", true)));
        } catch (IOException e) {
            remove.run();
        }
        return emitter;
    }

    public void send(Long userId, Object payload) {
        List<SseEmitter> list = emitters.get(userId);
        if (list == null) {
            return;
        }
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(payload));
            } catch (IOException | IllegalStateException e) {
                list.remove(emitter);
            }
        }
    }

    @Scheduled(fixedDelayString = "${app.notifications.sse-heartbeat:25s}")
    public void heartbeat() {
        emitters.values()
                .forEach(list -> list.forEach(emitter -> {
                    try {
                        emitter.send(SseEmitter.event().comment("hb"));
                    } catch (IOException | IllegalStateException e) {
                        list.remove(emitter);
                    }
                }));
    }

    public int openConnections() {
        return emitters.values().stream().mapToInt(List::size).sum();
    }
}
