package br.com.nandesk.it;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.awaitility.Awaitility;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/** Caixa de saída de teste: guarda os e-mails em vez de enviar (RF-10). */
public class CapturedMail extends JavaMailSenderImpl {

    private static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9_\\-]+)");
    private final List<SimpleMailMessage> sent = new CopyOnWriteArrayList<>();

    @Override
    public void send(SimpleMailMessage... messages) {
        sent.addAll(List.of(messages));
    }

    /** Espera o e-mail (o envio é assíncrono) e devolve o token do link. */
    public String tokenSentTo(String email, String subjectFragment) {
        return Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .until(
                        () -> sent.stream()
                                .filter(m ->
                                        m.getTo() != null && List.of(m.getTo()).contains(email))
                                .filter(m -> m.getSubject() != null
                                        && m.getSubject().toLowerCase().contains(subjectFragment.toLowerCase()))
                                .reduce((first, second) -> second)
                                .map(m -> {
                                    Matcher matcher = TOKEN.matcher(m.getText());
                                    return matcher.find() ? matcher.group(1) : null;
                                })
                                .orElse(null),
                        token -> token != null);
    }
}
