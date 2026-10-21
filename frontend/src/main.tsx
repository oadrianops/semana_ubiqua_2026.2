import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from './app/App';
import './index.css';
import { initI18n } from './shared/i18n/config';
import { startVitals } from './shared/perf/vitals';

/**
 * Inicialização: o i18n do PT-BR já vem no pacote; nada aqui espera a rede para desenhar a tela (P4, RNF-15).
 */
async function bootstrap() {
  await initI18n();
  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <App />
    </StrictMode>,
  );
  startVitals();
}

void bootstrap();
