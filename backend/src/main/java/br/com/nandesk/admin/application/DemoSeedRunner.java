package br.com.nandesk.admin.application;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Carrega o seed na inicialização quando {@code app.seed.enabled=true} (padrão no perfil dev). Em produção fica
 * desligado; para a demonstração pública do hackathon a equipe pode ligá-lo com uma senha forte (ADR-0017).
 */
@Component
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
public class DemoSeedRunner implements ApplicationRunner {

    private final DemoSeedService seed;

    public DemoSeedRunner(DemoSeedService seed) {
        this.seed = seed;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed.load();
    }
}
