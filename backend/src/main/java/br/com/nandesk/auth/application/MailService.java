package br.com.nandesk.auth.application;

import br.com.nandesk.shared.config.AppProperties;
import br.com.nandesk.shared.config.AsyncConfig;
import br.com.nandesk.shared.web.Masking;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.MessageSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * E-mails transacionais (RF-10). Em desenvolvimento vão para o Mailpit; em produção para o SMTP configurado. Sem
 * SMTP ({@code app.mail.enabled=false}) nada é enviado e as contas são verificadas automaticamente (ADR-0017).
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final ObjectProvider<JavaMailSender> sender;
    private final MessageSource messages;
    private final AppProperties properties;

    public MailService(ObjectProvider<JavaMailSender> sender, MessageSource messages, AppProperties properties) {
        this.sender = sender;
        this.messages = messages;
        this.properties = properties;
    }

    public boolean enabled() {
        return properties.mail().enabled() && sender.getIfAvailable() != null;
    }

    @Async(AsyncConfig.SIDE_EFFECTS)
    public void sendVerification(String to, String name, String token, Locale locale) {
        String link = properties.publicBaseUrl() + "/verify-email?token=" + token;
        send(to, "mail.verify.subject", "mail.verify.body", new Object[] {name, link}, locale);
    }

    @Async(AsyncConfig.SIDE_EFFECTS)
    public void sendPasswordReset(String to, String name, String token, Locale locale) {
        String link = properties.publicBaseUrl() + "/reset-password?token=" + token;
        send(to, "mail.reset.subject", "mail.reset.body", new Object[] {name, link}, locale);
    }

    private void send(String to, String subjectKey, String bodyKey, Object[] args, Locale locale) {
        JavaMailSender mail = sender.getIfAvailable();
        if (!properties.mail().enabled() || mail == null) {
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.mail().from());
            message.setTo(to);
            message.setSubject(messages.getMessage(subjectKey, null, locale));
            message.setText(messages.getMessage(bodyKey, args, locale));
            mail.send(message);
        } catch (RuntimeException e) {
            log.warn("Falha ao enviar e-mail para {}: {}", Masking.email(to), e.getMessage());
        }
    }
}
