package br.com.nandesk.notification.application;

import br.com.nandesk.shared.config.AppProperties;
import br.com.nandesk.shared.crypto.Hashing;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Tickets de uso único para o SSE (9.8): o {@code EventSource} do navegador não envia cabeçalhos, então o app pede
 * um ticket válido por 60 s e o usa na URL do stream.
 */
@Component
public class StreamTickets {

    private final Cache<String, Long> tickets;

    public StreamTickets(AppProperties properties) {
        this.tickets = Caffeine.newBuilder()
                .expireAfterWrite(properties.notifications().streamTicketTtl())
                .maximumSize(50_000)
                .build();
    }

    public String issue(Long userId) {
        String ticket = Hashing.randomToken();
        tickets.put(ticket, userId);
        return ticket;
    }

    /** Consome o ticket: só funciona uma vez. */
    public Optional<Long> redeem(String ticket) {
        if (ticket == null) {
            return Optional.empty();
        }
        Long userId = tickets.asMap().remove(ticket);
        return Optional.ofNullable(userId);
    }
}
