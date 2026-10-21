import { BellRing, LocateFixed } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { toast } from '../../../shared/ui/toast';
import type { TaskType } from '../../../shared/api/types';
import { useNamespaces } from '../../../shared/i18n/useNamespaces';
import { formatMoney, formatNumber } from '../../../shared/lib/format';
import { useNetworkState } from '../../../shared/offline/network';
import { isPushSubscribed, pushSupport, subscribePush } from '../../../shared/pwa/push';
import { Button } from '../../../shared/ui/Button';
import { EmptyState, ErrorState, LoadingRows, OfflineState } from '../../../shared/ui/States';
import { useMarkets } from '../../public-api';
import { useAvailableTasks } from '../api';
import { MyActiveTasks } from '../MyActiveTasks';

const TYPES: TaskType[] = ['UPDATE_STALL', 'NEW_STALL', 'VERIFY_REPORT'];

/**
 * Tarefas disponíveis (RF-15) como um quadro ordenado por distância quando a pessoa permite (a posição só ordena e
 * não é gravada — RN-21). Inclui ativar notificações (RF-14) e as tarefas em andamento.
 */
export default function TasksPage() {
  useNamespaces(['collab']);
  const { t, i18n } = useTranslation('collab');
  const [market, setMarket] = useState<string | null>(null);
  const [type, setType] = useState<TaskType | null>(null);
  const [position, setPosition] = useState<{ lat: number; lng: number } | null>(null);
  const [locating, setLocating] = useState(false);
  const network = useNetworkState();
  const markets = useMarkets();
  const tasks = useAvailableTasks({ market, type, lat: position?.lat ?? null, lng: position?.lng ?? null });
  const [pushState, setPushState] = useState<'unknown' | 'on' | 'off'>('unknown');
  const support = pushSupport();

  useEffect(() => {
    void isPushSubscribed().then((on) => setPushState(on ? 'on' : 'off'));
  }, []);

  function sortByDistance() {
    setLocating(true);
    navigator.geolocation?.getCurrentPosition(
      (p) => {
        setPosition({ lat: p.coords.latitude, lng: p.coords.longitude });
        setLocating(false);
      },
      () => {
        setLocating(false);
        toast.error(t('tasks.locationDenied'));
      },
      { enableHighAccuracy: false, timeout: 10_000, maximumAge: 300_000 },
    );
  }

  async function enablePush() {
    try {
      const result = await subscribePush();
      if (result === 'subscribed') {
        setPushState('on');
        toast.success(t('tasks.pushOn'));
      } else if (result === 'denied') toast.error(t('tasks.pushDenied'));
      else toast.message(t('tasks.pushDisabled'));
    } catch {
      toast.error(t('tasks.pushError'));
    }
  }

  return (
    <div className="safe-x mx-auto grid max-w-[var(--content-max)] gap-6 py-6">
      <header className="grid gap-2">
        <h1 className="text-3xl">{t('tasks.title')}</h1>
        <p className="max-w-[var(--measure)] text-support">{t('tasks.lead')}</p>
      </header>

      <MyActiveTasks />

      {pushState === 'off' && (
        <div className="grid gap-2 rounded-[var(--radius-panel)] bg-soft p-4 md:flex md:items-center md:justify-between">
          <p className="text-ink">
            {support === 'needs-install' ? t('tasks.pushIos') : t('tasks.pushOffer')}
          </p>
          {support === 'supported' && (
            <Button
              variant="secondary"
              icon={<BellRing size={18} aria-hidden="true" />}
              onClick={() => void enablePush()}
            >
              {t('tasks.enablePush')}
            </Button>
          )}
        </div>
      )}

      <div className="flex flex-wrap items-end gap-3">
        <label className="grid gap-1">
          <span className="text-sm font-semibold text-support">{t('tasks.market')}</span>
          <select
            value={market ?? ''}
            onChange={(e) => setMarket(e.target.value || null)}
            className="min-h-11 rounded-[var(--radius-control)] border border-line-strong bg-surface px-3"
          >
            <option value="">{t('tasks.allMarkets')}</option>
            {markets.data?.map((m) => (
              <option key={m.slug} value={m.slug}>
                {m.name}
              </option>
            ))}
          </select>
        </label>
        <label className="grid gap-1">
          <span className="text-sm font-semibold text-support">{t('tasks.type')}</span>
          <select
            value={type ?? ''}
            onChange={(e) => setType((e.target.value || null) as TaskType | null)}
            className="min-h-11 rounded-[var(--radius-control)] border border-line-strong bg-surface px-3"
          >
            <option value="">{t('tasks.allTypes')}</option>
            {TYPES.map((ty) => (
              <option key={ty} value={ty}>
                {t(`type.${ty}`)}
              </option>
            ))}
          </select>
        </label>
        <Button
          variant="ghost"
          icon={<LocateFixed size={18} aria-hidden="true" />}
          busy={locating}
          onClick={sortByDistance}
        >
          {position ? t('tasks.sortedByDistance') : t('tasks.sortByDistance')}
        </Button>
      </div>

      {tasks.data && !tasks.data.canAccept && (
        <p className="rounded-[var(--radius-control)] bg-soft p-3 font-semibold text-warning">
          {t(`blocked.${tasks.data.blockedReason}`)}
        </p>
      )}

      <section aria-labelledby="open-tasks" className="grid">
        <h2
          id="open-tasks"
          className="flex items-baseline justify-between border-b-2 border-ink pb-1 text-xl"
        >
          {t('tasks.openTitle')}
          {tasks.data && (
            <span className="text-sm font-normal text-support">
              {t('tasks.count', { count: tasks.data.items.length })}
            </span>
          )}
        </h2>
        {network === 'offline' && !tasks.data && <OfflineState body={t('tasks.offline')} />}
        {tasks.isPending && network !== 'offline' && <LoadingRows rows={4} />}
        {tasks.isError && !tasks.data && network !== 'offline' && (
          <ErrorState error={tasks.error} onRetry={() => void tasks.refetch()} />
        )}
        {tasks.data && tasks.data.items.length === 0 && (
          <EmptyState title={t('tasks.emptyTitle')} body={t('tasks.emptyBody')} />
        )}
        <ul className="divide-y divide-line">
          {tasks.data?.items.map((task) => (
            <li key={task.publicId}>
              <Link
                to={`/collaborate/tasks/${task.publicId}`}
                className="grid grid-cols-[1fr_auto] gap-x-3 gap-y-0.5 py-3 text-ink no-underline hover:bg-soft"
              >
                <span className="font-semibold">
                  {t(`type.${task.type}`)}
                  {task.stall && (
                    <span className="display tabular ml-2 text-brand">
                      {t('common:stall.code', { code: task.stall.code })}
                    </span>
                  )}
                </span>
                <span className="display tabular row-span-2 self-center text-xl">
                  {formatMoney(task.commission, i18n.language)}
                </span>
                <span className="text-sm text-support">
                  {task.market.name}
                  {task.stall?.sectorName && ` · ${task.stall.sectorName}`}
                  {task.suggestedSector && !task.stall && ` · ${task.suggestedSector}`}
                  {task.distanceMeters != null &&
                    ` · ${t('tasks.distance', { km: formatNumber(task.distanceMeters / 1000, i18n.language, 1) })}`}
                </span>
              </Link>
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}
