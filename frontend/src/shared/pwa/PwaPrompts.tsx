import { Download, RefreshCw, Share, X } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useRegisterSW } from 'virtual:pwa-register/react';
import { idb, requestPersistentStorage } from '../offline/idb';
import { listItems } from '../offline/outbox';
import { Button } from '../ui/Button';
import { installSignals } from './install-signals';

interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>;
}

const DISMISS_KEY = 'install-dismissed-at';

function isStandalone() {
  return (
    window.matchMedia('(display-mode: standalone)').matches ||
    (navigator as Navigator & { standalone?: boolean }).standalone === true
  );
}

function isIos() {
  return /iphone|ipad|ipod/i.test(navigator.userAgent) && !/crios|fxios/i.test(navigator.userAgent);
}

/**
 * Convite de instalação e aviso de atualização (RF-35). O convite só aparece depois de uso real (segunda visita
 * ou uma busca com resultado), nunca na primeira tela da primeira visita; é fácil de dispensar e fica lembrado.
 * A versão nova só ativa no toque em "Atualizar" e nunca no meio de um envio.
 */
export function PwaPrompts() {
  const { t } = useTranslation();
  const [deferred, setDeferred] = useState<BeforeInstallPromptEvent | null>(null);
  const [showInstall, setShowInstall] = useState(false);
  const [iosHelp, setIosHelp] = useState(false);
  const {
    needRefresh: [needRefresh, setNeedRefresh],
    updateServiceWorker,
  } = useRegisterSW({ immediate: true });

  useEffect(() => {
    const onPrompt = (e: Event) => {
      e.preventDefault();
      setDeferred(e as BeforeInstallPromptEvent);
    };
    const onInstalled = () => {
      setShowInstall(false);
      void requestPersistentStorage();
    };
    window.addEventListener('beforeinstallprompt', onPrompt);
    window.addEventListener('appinstalled', onInstalled);
    return () => {
      window.removeEventListener('beforeinstallprompt', onPrompt);
      window.removeEventListener('appinstalled', onInstalled);
    };
  }, []);

  useEffect(() => {
    if (isStandalone()) return;
    let cancelled = false;
    const evaluate = async () => {
      const dismissed = await idb.get<string>('prefs', DISMISS_KEY);
      const recentlyDismissed = dismissed && Date.now() - new Date(dismissed).getTime() < 14 * 86_400_000;
      if (!cancelled && !recentlyDismissed && (await installSignals.qualifies())) {
        setShowInstall(true);
      }
    };
    void evaluate();
    const unsubscribe = installSignals.subscribe(() => void evaluate());
    return () => {
      cancelled = true;
      unsubscribe();
    };
  }, []);

  async function install() {
    if (deferred) {
      await deferred.prompt();
      await deferred.userChoice;
      setDeferred(null);
      setShowInstall(false);
    } else if (isIos()) {
      setIosHelp(true);
    }
  }

  async function dismiss() {
    setShowInstall(false);
    await idb.set('prefs', DISMISS_KEY, new Date().toISOString());
  }

  async function update() {
    const busy = (await listItems()).some((i) => i.state === 'UPLOADING_PHOTOS' || i.state === 'SUBMITTING');
    if (busy) return;
    await updateServiceWorker(true);
  }

  const canInstall = showInstall && (deferred || isIos());

  return (
    <div
      className="pointer-events-none fixed inset-x-0 bottom-20 z-30 grid gap-2 px-4 md:bottom-4"
      aria-live="polite"
    >
      {needRefresh && (
        <div className="pointer-events-auto mx-auto flex w-full max-w-md items-center gap-3 rounded-[var(--radius-panel)] border border-line bg-surface p-3 shadow-[var(--shadow-dialog)]">
          <p className="flex-1 text-sm font-semibold text-ink">{t('pwa.updateAvailable')}</p>
          <Button size="sm" icon={<RefreshCw size={16} aria-hidden="true" />} onClick={() => void update()}>
            {t('pwa.update')}
          </Button>
          <button
            type="button"
            className="inline-flex size-11 items-center justify-center rounded-[var(--radius-control)] text-support hover:bg-soft"
            onClick={() => setNeedRefresh(false)}
            aria-label={t('action.close')}
          >
            <X size={18} aria-hidden="true" />
          </button>
        </div>
      )}
      {canInstall && (
        <div className="pointer-events-auto mx-auto w-full max-w-md rounded-[var(--radius-panel)] border border-line bg-surface p-3 shadow-[var(--shadow-dialog)]">
          <div className="flex items-start gap-3">
            <img src="/brand/png/pwa-192x192.png" width={40} height={40} alt="" className="rounded-lg" />
            <div className="flex-1">
              <p className="font-semibold text-ink">{t('pwa.installTitle')}</p>
              <p className="text-sm text-support">{t('pwa.installBody')}</p>
            </div>
          </div>
          {iosHelp ? (
            <p className="mt-3 flex items-start gap-2 text-sm text-ink">
              <Share size={18} aria-hidden="true" className="mt-0.5 shrink-0" />
              {t('pwa.iosHelp')}
            </p>
          ) : (
            <div className="mt-3 flex gap-2">
              <Button
                size="sm"
                icon={<Download size={16} aria-hidden="true" />}
                onClick={() => void install()}
              >
                {t('pwa.install')}
              </Button>
              <Button size="sm" variant="ghost" onClick={() => void dismiss()}>
                {t('pwa.notNow')}
              </Button>
            </div>
          )}
          {iosHelp && (
            <Button size="sm" variant="ghost" className="mt-1" onClick={() => void dismiss()}>
              {t('pwa.notNow')}
            </Button>
          )}
        </div>
      )}
    </div>
  );
}
