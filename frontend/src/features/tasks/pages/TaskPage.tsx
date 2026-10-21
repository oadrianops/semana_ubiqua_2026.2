import { Sheet } from '../../../shared/ui/Sheet';
import { useDocumentTitle } from '../../../shared/lib/useDocumentTitle';
import { CheckCircle2, Inbox, LocateFixed, MapPin } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate, useParams } from 'react-router';
import { toast } from '../../../shared/ui/toast';
import { ApiError } from '../../../shared/api/http';
import type { TaskDetail } from '../../../shared/api/types';
import { useNamespaces } from '../../../shared/i18n/useNamespaces';
import { formatMoney, formatNumber } from '../../../shared/lib/format';
import { getItem, removeItem, subscribeOutbox, type OutboxItem } from '../../../shared/offline/outbox';
import { Button } from '../../../shared/ui/Button';
import { ErrorState, LoadingRows } from '../../../shared/ui/States';
import { locate, useTask, useTaskActions } from '../api';
import { DeadlineBar, Stepper } from '../Stepper';
import { SubmissionForm } from '../SubmissionForm';

/**
 * Tarefa em 3 passos (CA-2, 14.3): Aceitar → Fazer check-in → Enviar. Progresso e prazos sempre visíveis; o
 * envio vai para a fila e segue sozinho quando houver sinal (RF-18).
 */
export default function TaskPage() {
  useNamespaces(['collab']);
  const { publicId = '' } = useParams();
  const { t, i18n } = useTranslation('collab');
  const navigate = useNavigate();
  const task = useTask(publicId);
  const { accept, checkIn, release } = useTaskActions(publicId);
  const [outbox, setOutbox] = useState<OutboxItem | undefined>();
  const [geoError, setGeoError] = useState<string | null>(null);
  const [locating, setLocating] = useState(false);
  const [confirmRelease, setConfirmRelease] = useState(false);
  useDocumentTitle(task.data ? t(`type.${task.data.type}`) : t('tasks.title'));

  useEffect(() => {
    const load = () => void getItem(publicId).then(setOutbox);
    load();
    return subscribeOutbox(load);
  }, [publicId]);

  if (!task.data) {
    return (
      <div className="safe-x mx-auto max-w-3xl py-8">
        {task.isPending ? (
          <LoadingRows rows={5} />
        ) : (
          <ErrorState error={task.error} onRetry={() => void task.refetch()} />
        )}
      </div>
    );
  }

  const data = task.data;
  const mine = data.myAssignment;
  const step: 1 | 2 | 3 | 4 =
    outbox && outbox.state !== 'NEEDS_ACTION'
      ? 4
      : data.status === 'CHECKED_IN' && mine
        ? 3
        : data.status === 'ACCEPTED' && mine
          ? 2
          : data.status === 'SUBMITTED'
            ? 4
            : 1;

  async function onAccept() {
    try {
      await accept.mutateAsync();
      toast.success(t('accept.done'));
    } catch (e) {
      toast.error(e instanceof ApiError ? (e.problem.detail ?? t('accept.failed')) : t('accept.failed'));
    }
  }

  async function onCheckIn() {
    setGeoError(null);
    setLocating(true);
    try {
      const pos = await locate();
      const result = await checkIn.mutateAsync({
        latitude: pos.coords.latitude,
        longitude: pos.coords.longitude,
        accuracyMeters: pos.coords.accuracy,
      });
      toast.success(result.flag ? t('checkIn.doneLow') : t('checkIn.done'));
    } catch (e) {
      if (e instanceof ApiError) {
        const distance = e.problem.distanceMeters as number | undefined;
        setGeoError(
          e.code === 'CHECKIN_OUT_OF_RANGE' && distance != null
            ? t('checkIn.outOfRange', { km: formatNumber(distance / 1000, i18n.language, 1) })
            : (e.problem.detail ?? t('checkIn.failed')),
        );
      } else {
        const code = (e as GeolocationPositionError)?.code;
        setGeoError(
          code === 1 ? t('checkIn.denied') : code === 3 ? t('checkIn.timeout') : t('checkIn.unavailable'),
        );
      }
    } finally {
      setLocating(false);
    }
  }

  async function onRelease() {
    try {
      await release.mutateAsync();
      await removeItem(publicId);
      toast.message(t('release.done'));
      navigate('/collaborate');
    } catch (e) {
      toast.error(e instanceof ApiError ? (e.problem.detail ?? t('release.failed')) : t('release.failed'));
    }
  }

  return (
    <div className="safe-x mx-auto grid max-w-3xl gap-6 py-6">
      <p className="text-sm">
        <Link to="/collaborate" className="inline-flex min-h-11 items-center">
          {t('tasks.title')}
        </Link>
      </p>
      <Stepper current={step} />
      <TaskSummary data={data} />

      {mine && data.status === 'ACCEPTED' && (
        <DeadlineBar
          start={mine.acceptedAt}
          deadline={mine.checkinDeadlineAt}
          label={t('deadline.checkIn')}
        />
      )}
      {mine && data.status === 'CHECKED_IN' && mine.checkedInAt && mine.submissionDeadlineAt && !outbox && (
        <DeadlineBar
          start={mine.checkedInAt}
          deadline={mine.submissionDeadlineAt}
          label={t('deadline.submit')}
        />
      )}

      {step === 1 && data.status === 'OPEN' && (
        <section className="grid gap-3">
          <p className="text-support">{t('accept.body')}</p>
          <Button size="lg" block busy={accept.isPending} onClick={() => void onAccept()}>
            {t('accept.action', { amount: formatMoney(data.commission, i18n.language) })}
          </Button>
        </section>
      )}

      {step === 2 && (
        <section className="grid gap-3">
          <p className="flex items-start gap-2 text-ink">
            <MapPin size={20} aria-hidden="true" className="mt-0.5 shrink-0 text-brand" />
            {t('checkIn.body', { market: data.market.name, radius: data.market.radiusMeters })}
          </p>
          <Button
            size="lg"
            block
            busy={locating || checkIn.isPending}
            icon={<LocateFixed size={20} aria-hidden="true" />}
            onClick={() => void onCheckIn()}
          >
            {t('checkIn.action')}
          </Button>
          {geoError && (
            <div className="grid gap-1 rounded-[var(--radius-control)] bg-soft p-3" role="alert">
              <p className="font-semibold text-danger">{geoError}</p>
              <p className="text-sm text-support">{t('checkIn.help')}</p>
            </div>
          )}
        </section>
      )}

      {step === 3 && <SubmissionForm task={data} />}

      {step === 4 && <SubmittedState item={outbox} />}

      {outbox?.state === 'NEEDS_ACTION' && (
        <div className="grid gap-2 rounded-[var(--radius-control)] bg-soft p-3" role="alert">
          <p className="font-semibold text-danger">
            {t(`needsAction.${outbox.lastError ?? 'OTHER'}`, { defaultValue: t('needsAction.OTHER') })}
          </p>
          <Button variant="secondary" onClick={() => void removeItem(publicId)}>
            {t('needsAction.discard')}
          </Button>
        </div>
      )}

      {mine && (data.status === 'ACCEPTED' || data.status === 'CHECKED_IN') && !outbox && (
        <Button variant="ghost" className="justify-self-start" onClick={() => setConfirmRelease(true)}>
          {data.status === 'CHECKED_IN' ? t('release.afterCheckIn') : t('release.action')}
        </Button>
      )}
      <Sheet
        open={confirmRelease}
        onOpenChange={setConfirmRelease}
        title={t('release.confirmTitle')}
        description={data.status === 'CHECKED_IN' ? t('release.confirmBodyAfter') : t('release.confirmBody')}
      >
        <div className="grid gap-3">
          <Button variant="danger" block busy={release.isPending} onClick={() => void onRelease()}>
            {t('release.confirm')}
          </Button>
          <Button variant="secondary" block onClick={() => setConfirmRelease(false)}>
            {t('release.keep')}
          </Button>
        </div>
      </Sheet>
    </div>
  );
}

function TaskSummary({ data }: { data: TaskDetail }) {
  const { t, i18n } = useTranslation('collab');
  return (
    <header className="grid gap-2">
      <h1 className="text-3xl">{t(`type.${data.type}`)}</h1>
      <dl className="grid gap-1">
        <div className="price-row">
          <dt className="font-semibold">{t('summary.market')}</dt>
          <span className="price-row__leader" aria-hidden="true" />
          <dd>{data.market.name}</dd>
        </div>
        {data.stall && (
          <div className="price-row">
            <dt className="font-semibold">{t('summary.stall')}</dt>
            <span className="price-row__leader" aria-hidden="true" />
            <dd>
              <span className="display tabular text-brand">
                {t('common:stall.code', { code: data.stall.code })}
              </span>
              {data.stall.name && ` · ${data.stall.name}`}
            </dd>
          </div>
        )}
        {(data.stall?.sectorName ?? data.suggestedSector) && (
          <div className="price-row">
            <dt className="font-semibold">{t('summary.sector')}</dt>
            <span className="price-row__leader" aria-hidden="true" />
            <dd>{data.stall?.sectorName ?? data.suggestedSector}</dd>
          </div>
        )}
        <div className="price-row">
          <dt className="font-semibold">{t('summary.commission')}</dt>
          <span className="price-row__leader" aria-hidden="true" />
          <dd className="display tabular text-xl">{formatMoney(data.commission, i18n.language)}</dd>
        </div>
      </dl>
      {data.instructions && (
        <p className="rounded-[var(--radius-control)] bg-soft p-3 text-ink">{data.instructions}</p>
      )}
      {data.status === 'OPEN' && <p className="text-sm text-support">{t('summary.deadlines')}</p>}
    </header>
  );
}

function SubmittedState({ item }: { item?: OutboxItem }) {
  const { t } = useTranslation('collab');
  const sent = !item || item.state === 'SENT';
  return (
    <section
      className="grid gap-2 rounded-[var(--radius-panel)] bg-soft p-4"
      role="status"
      aria-live="polite"
    >
      <p className="flex items-center gap-2 text-lg font-semibold text-ink">
        {sent ? (
          <CheckCircle2 size={22} aria-hidden="true" className="text-success" />
        ) : (
          <Inbox size={22} aria-hidden="true" />
        )}
        {sent ? t('submitted.received') : t('submitted.queued')}
      </p>
      {!sent && <p className="text-support">{t('submit.queuedHint')}</p>}
      {item && item.state !== 'SENT' && (
        <ul className="grid gap-1 text-sm text-support">
          {item.photos.map((p, i) => (
            <li key={p.clientUploadId}>
              {t('photo.label', { n: i + 1 })}: {t(`photo.state.${p.state}`)}
            </li>
          ))}
          <li>{t(`outbox.${item.state}`)}</li>
        </ul>
      )}
      <Link to="/collaborate" className="font-semibold">
        {t('submitted.back')}
      </Link>
    </section>
  );
}
