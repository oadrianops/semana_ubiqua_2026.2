import { CloudOff, Gauge, Inbox } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { listItems, subscribeOutbox } from '../shared/offline/outbox';
import { useNetworkState } from '../shared/offline/network';

/**
 * Estados de rede (9.13): faixa discreta sem conexão, aviso de conexão lenta e contagem da fila de envios
 * ("Na fila: 2 envios aguardando sinal"). Mensagens em aria-live.
 */
export function NetworkBanner() {
  const { t } = useTranslation();
  const state = useNetworkState();
  const [queued, setQueued] = useState(0);
  const [sentJustNow, setSentJustNow] = useState(0);

  useEffect(() => {
    let previous = 0;
    const update = async () => {
      const items = await listItems();
      const pending = items.filter((i) => i.state !== 'SENT' && i.state !== 'DRAFT').length;
      if (previous > pending) setSentJustNow(previous - pending);
      previous = pending;
      setQueued(pending);
    };
    void update();
    return subscribeOutbox(() => void update());
  }, []);

  useEffect(() => {
    if (!sentJustNow) return;
    const timer = setTimeout(() => setSentJustNow(0), 6000);
    return () => clearTimeout(timer);
  }, [sentJustNow]);

  const message =
    state === 'offline'
      ? { icon: <CloudOff size={18} aria-hidden="true" />, text: t('network.offline') }
      : state === 'slow'
        ? { icon: <Gauge size={18} aria-hidden="true" />, text: t('network.slow') }
        : null;

  return (
    <div aria-live="polite" role="status">
      {message && (
        <p className="safe-x flex items-center justify-center gap-2 border-b border-line bg-brand-soft py-2 text-sm font-semibold text-ink">
          {message.icon}
          {message.text}
        </p>
      )}
      {queued > 0 && (
        <p className="safe-x flex items-center justify-center gap-2 border-b border-line bg-soft py-2 text-sm text-ink">
          <Inbox size={18} aria-hidden="true" />
          {t('network.queued', { count: queued })}
        </p>
      )}
      {sentJustNow > 0 && queued === 0 && (
        <p className="safe-x border-b border-line bg-soft py-2 text-center text-sm font-semibold text-success">
          {t('network.sent', { count: sentJustNow })}
        </p>
      )}
    </div>
  );
}
