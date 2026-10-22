import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate, useParams } from 'react-router';
import { toast } from '../../../shared/ui/toast';
import { api, fetchImageObjectUrl } from '../../../shared/api/http';
import { cn } from '../../../shared/lib/cn';
import { formatDateTime, formatMoney, formatNumber } from '../../../shared/lib/format';
import { Button } from '../../../shared/ui/Button';
import { CheckboxField, SelectField, TextareaField } from '../../../shared/ui/Field';
import { Sheet } from '../../../shared/ui/Sheet';
import { ErrorState, LoadingRows } from '../../../shared/ui/States';
import { REJECTION_REASONS, type PhotoView, type RejectionReason, type SubmissionDetail } from '../types';
import { AdminHeader, errorMessage, Pill, SectionTitle, show } from '../ui';

/**
 * Revisão de um envio (RF-22): antes e depois lado a lado, conflitos de versão (RN-26), check-in, fotos
 * restritas e as decisões. Quem enviou não modera o próprio envio (RN-11).
 */
export default function ReviewPage() {
  const { publicId = '' } = useParams();
  const { t, i18n } = useTranslation('admin');
  const lng = i18n.language;
  const navigate = useNavigate();
  const client = useQueryClient();
  const query = useQuery({
    queryKey: ['admin', 'submission', publicId],
    queryFn: () => api.get<SubmissionDetail>(`/moderation/submissions/${publicId}`),
    networkMode: 'online',
  });
  const [acceptConflicts, setAcceptConflicts] = useState(false);
  const [rejectOpen, setRejectOpen] = useState(false);

  const done = (message: string) => {
    toast.success(message);
    void client.invalidateQueries({ queryKey: ['admin'] });
    navigate('/admin/moderation');
  };
  const approve = useMutation({
    mutationFn: () =>
      api.post(
        `/moderation/submissions/${publicId}/approve`,
        { finalChangeSet: null, acceptConflicts },
        { retries: 0 },
      ),
    onSuccess: () => done(t('review.approved')),
    onError: (e) => toast.error(errorMessage(e, t('review.error'))),
  });

  if (query.isPending) return <LoadingRows rows={8} />;
  if (query.isError) return <ErrorState error={query.error} onRetry={() => void query.refetch()} />;
  const s = query.data;
  const pending = s.status === 'PENDING';
  // Conflito só vale antes da decisão; o servidor manda as chaves ("stall", "offering:<slug>"), aqui viram nomes.
  const hasConflicts = pending && s.conflicts.length > 0;
  const conflictLabel = (key: string) => {
    if (key === 'stall') return t('review.stallChanges');
    const slug = key.startsWith('offering:') ? key.slice('offering:'.length) : key;
    return s.offerings.find((o) => o.productSlug === slug)?.productName ?? slug;
  };

  return (
    <div className="grid gap-8">
      <p className="text-sm">
        <Link to="/admin/moderation" className="inline-flex min-h-11 items-center">
          {t('queue.title')}
        </Link>
      </p>
      <AdminHeader
        title={t(`collab:type.${s.type}`)}
        lead={
          <>
            {s.marketName}
            {s.stallCode && ` · ${t('common:stall.code', { code: s.stallCode })}`} ·{' '}
            {formatDateTime(s.createdAt, lng)}
          </>
        }
        actions={
          <Pill tone={s.status === 'APPROVED' ? 'good' : s.status === 'REJECTED' ? 'bad' : 'brand'}>
            {t(`collab:submission.${s.status}`)}
          </Pill>
        }
      />

      <dl className="grid gap-x-8 sm:grid-cols-2">
        {[
          [
            t('review.collaborator'),
            `${s.collaboratorName} · ${t('review.reputation', { score: s.collaboratorReputation })}`,
          ],
          [t('review.commission'), formatMoney(s.commission, lng) ?? '—'],
          [
            t('review.checkIn'),
            s.checkIn?.distanceMeters != null
              ? t('review.checkInValue', {
                  distance: formatNumber(s.checkIn.distanceMeters, lng),
                  accuracy:
                    s.checkIn.accuracyMeters == null ? '—' : formatNumber(s.checkIn.accuracyMeters, lng),
                })
              : '—',
          ],
        ].map(([label, value]) => (
          <div key={label} className="price-row py-2">
            <dt className="font-semibold">{label}</dt>
            <span className="price-row__leader" aria-hidden="true" />
            <dd className="tabular">{value}</dd>
          </div>
        ))}
      </dl>
      {s.checkIn?.flag && (
        <p className="rounded-[var(--radius-control)] bg-soft p-3 font-semibold text-warning">
          {t('review.lowAccuracyBody')}
        </p>
      )}

      {hasConflicts && (
        <section
          className="grid gap-2 rounded-[var(--radius-panel)] border-2 border-warning p-4"
          aria-labelledby="conflicts"
        >
          <h2 id="conflicts" className="text-lg">
            {t('review.conflictsTitle')}
          </h2>
          <p className="text-support">{t('review.conflictsBody')}</p>
          <ul className="list-disc pl-5">
            {s.conflicts.map((c) => (
              <li key={c}>{conflictLabel(c)}</li>
            ))}
          </ul>
        </section>
      )}

      {s.stallDiff.length > 0 && (
        <section className="grid gap-2">
          <SectionTitle>{t('review.stallChanges')}</SectionTitle>
          <DiffTable
            label={t('review.stallChanges')}
            rows={s.stallDiff.map((d) => ({
              key: d.field,
              label: t(`review.field.${d.field}`, { defaultValue: d.field }),
              before: show(d.before),
              after: show(d.after),
            }))}
          />
        </section>
      )}

      {s.offerings.length > 0 && (
        <section className="grid gap-2">
          <SectionTitle>{t('review.offerings')}</SectionTitle>
          <DiffTable
            label={t('review.offerings')}
            rows={s.offerings.map((o, i) => ({
              key: `${o.productSlug ?? 'new'}-${i}`,
              label: (
                <>
                  {o.productName}{' '}
                  <Pill
                    tone={o.action === 'REMOVE' ? 'bad' : o.action.startsWith('ADD') ? 'good' : 'neutral'}
                  >
                    {t(`review.action.${o.action}`, { defaultValue: o.action })}
                  </Pill>
                  {o.conflict && <Pill tone="warn">{t('review.conflict')}</Pill>}
                </>
              ),
              before: offeringText(o.before, t, lng),
              // Confirmar mantém o que estava; remover tira a oferta do box.
              after:
                o.action === 'CONFIRM'
                  ? offeringText(o.before, t, lng)
                  : o.action === 'REMOVE'
                    ? '—'
                    : offeringText(o.after, t, lng),
            }))}
          />
        </section>
      )}

      {s.changeSet.verification && (
        <section className="grid gap-2">
          <SectionTitle>{t('review.verification')}</SectionTitle>
          <p className="font-semibold">
            {s.changeSet.verification.problemConfirmed
              ? t('review.problemConfirmed')
              : t('review.problemNotConfirmed')}
          </p>
          {s.changeSet.verification.comment && (
            <p className="text-support">{s.changeSet.verification.comment}</p>
          )}
        </section>
      )}

      {s.notes && (
        <section className="grid gap-2">
          <SectionTitle>{t('review.notes')}</SectionTitle>
          <p className="text-ink">{s.notes}</p>
        </section>
      )}

      <section className="grid gap-3">
        <SectionTitle aside={t('review.photosHint')}>
          {t('review.photos', { count: s.photos.length })}
        </SectionTitle>
        <ul className="grid grid-cols-2 gap-3 md:grid-cols-3">
          {s.photos.map((p, i) => (
            <RestrictedPhoto key={p.fileId} photo={p} index={i} />
          ))}
        </ul>
      </section>

      {s.rejectionReason && (
        <p className="rounded-[var(--radius-control)] bg-soft p-3">
          <strong>{t(`collab:rejection.${s.rejectionReason}`)}</strong>
          {s.reviewComment && ` · ${s.reviewComment}`}
        </p>
      )}

      {pending && (
        <section className="sticky bottom-0 grid gap-3 border-t-2 border-ink bg-bg py-4">
          {s.ownSubmission ? (
            <p className="font-semibold text-warning">{t('review.ownSubmission')}</p>
          ) : (
            <>
              {hasConflicts && (
                <CheckboxField
                  label={t('review.acceptConflicts')}
                  checked={acceptConflicts}
                  onChange={(e) => setAcceptConflicts(e.target.checked)}
                />
              )}
              <div className="flex flex-wrap gap-3">
                <Button
                  size="lg"
                  busy={approve.isPending}
                  disabled={hasConflicts && !acceptConflicts}
                  onClick={() => approve.mutate()}
                >
                  {t('review.approve', { amount: formatMoney(s.commission, lng) })}
                </Button>
                <Button size="lg" variant="danger" onClick={() => setRejectOpen(true)}>
                  {t('review.reject')}
                </Button>
              </div>
            </>
          )}
        </section>
      )}
      <RejectSheet
        open={rejectOpen}
        onOpenChange={setRejectOpen}
        publicId={publicId}
        onDone={() => done(t('review.rejected'))}
      />
    </div>
  );
}

/** "R$ 8,00 a garrafa · disponível" a partir do mapa de uma oferta. */
function offeringText(
  offer: Record<string, unknown> | null,
  t: (key: string) => string,
  lng: string,
): string {
  if (!offer) return '—';
  const price = offer.price as { amount: string; currency: string } | null | undefined;
  const unit = offer.priceUnit as string | null | undefined;
  const availability = offer.availability as string | null | undefined;
  const parts = [
    [price ? formatMoney(price, lng) : t('common:price.ask'), unit ? t(`common:unitShort.${unit}`) : null]
      .filter(Boolean)
      .join(' '),
    availability ? t(`common:availability.${availability}`) : null,
  ].filter(Boolean);
  return parts.join(' · ') || '—';
}

function DiffTable({
  label,
  rows,
}: {
  label: string;
  rows: { key: string; label: React.ReactNode; before: string; after: string }[];
}) {
  const { t } = useTranslation('admin');
  // No celular a tabela rola para o lado: a região recebe foco para rolar pelo teclado (WCAG 2.1.1).
  return (
    <div className="overflow-x-auto" role="region" aria-label={label} tabIndex={0}>
      <table className="w-full min-w-[32rem] border-collapse text-left">
        <thead>
          <tr className="border-b border-line text-sm text-support">
            <th scope="col" className="py-2 pr-3 font-semibold">
              {t('review.item')}
            </th>
            <th scope="col" className="py-2 pr-3 font-semibold">
              {t('review.before')}
            </th>
            <th scope="col" className="py-2 font-semibold">
              {t('review.after')}
            </th>
          </tr>
        </thead>
        <tbody className="divide-y divide-line">
          {rows.map((r) => (
            <tr key={r.key} className="align-top">
              <th scope="row" className="py-2 pr-3 font-semibold">
                {r.label}
              </th>
              <td
                className={cn(
                  'tabular py-2 pr-3 text-support',
                  r.before !== r.after && 'line-through decoration-1',
                )}
              >
                {r.before}
              </td>
              <td
                className={cn(
                  'tabular py-2 font-semibold',
                  r.before !== r.after ? 'text-brand' : 'text-muted',
                )}
              >
                {r.after}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

/** Fotos de envio são restritas (RN-13): baixadas com o token e mostradas por object URL. */
function RestrictedPhoto({ photo, index }: { photo: PhotoView; index: number }) {
  const { t } = useTranslation('admin');
  const [url, setUrl] = useState<string | null>(null);
  const [failed, setFailed] = useState(false);
  useEffect(() => {
    let alive = true;
    let created: string | null = null;
    fetchImageObjectUrl(photo.thumbUrl ?? photo.url)
      .then((u) => {
        created = u;
        if (alive) setUrl(u);
        else URL.revokeObjectURL(u);
      })
      .catch(() => alive && setFailed(true));
    return () => {
      alive = false;
      if (created) URL.revokeObjectURL(created);
    };
  }, [photo.thumbUrl, photo.url]);

  async function openFull() {
    try {
      const full = await fetchImageObjectUrl(photo.url);
      window.open(full, '_blank', 'noopener');
      setTimeout(() => URL.revokeObjectURL(full), 60_000);
    } catch {
      toast.error(t('review.photoError'));
    }
  }

  return (
    <li className="grid gap-1">
      <button
        type="button"
        onClick={() => void openFull()}
        className="overflow-hidden rounded-[var(--radius-control)] bg-soft"
        aria-label={t('review.openPhoto', { n: index + 1 })}
      >
        {url ? (
          <img
            src={url}
            alt={t('review.photoAlt', { n: index + 1, role: t(`collab:photo.roles.${photo.role}`) })}
            className="aspect-[4/3] w-full object-cover"
          />
        ) : (
          <span className="flex aspect-[4/3] items-center justify-center text-sm text-support">
            {failed ? t('review.photoError') : t('common:state.loading')}
          </span>
        )}
      </button>
      <span className="flex flex-wrap gap-1 text-sm text-support">
        {t(`collab:photo.roles.${photo.role}`)}
        {photo.duplicateCount > 0 && (
          <Pill tone="warn">{t('review.duplicatePhoto', { count: photo.duplicateCount })}</Pill>
        )}
      </span>
    </li>
  );
}

function RejectSheet({
  open,
  onOpenChange,
  publicId,
  onDone,
}: {
  open: boolean;
  onOpenChange: (o: boolean) => void;
  publicId: string;
  onDone: () => void;
}) {
  const { t } = useTranslation('admin');
  const [reason, setReason] = useState<RejectionReason>('ILLEGIBLE_PHOTO');
  const [comment, setComment] = useState('');
  const [reopen, setReopen] = useState(true);
  const reject = useMutation({
    mutationFn: () =>
      api.post(
        `/moderation/submissions/${publicId}/reject`,
        { reason, comment: comment.trim() || null, reopenTask: reopen },
        { retries: 0 },
      ),
    onSuccess: () => {
      onOpenChange(false);
      onDone();
    },
    onError: (e) => toast.error(errorMessage(e, t('review.error'))),
  });
  const submit = (e: FormEvent) => {
    e.preventDefault();
    reject.mutate();
  };
  return (
    <Sheet
      open={open}
      onOpenChange={onOpenChange}
      title={t('review.rejectTitle')}
      description={t('review.rejectBody')}
    >
      <form onSubmit={submit} className="grid gap-4">
        <SelectField
          label={t('review.reason')}
          value={reason}
          onChange={(e) => setReason(e.target.value as RejectionReason)}
        >
          {REJECTION_REASONS.map((r) => (
            <option key={r} value={r}>
              {t(`collab:rejection.${r}`)}
            </option>
          ))}
        </SelectField>
        <TextareaField
          label={t('review.comment')}
          hint={t('review.commentHint')}
          maxLength={500}
          value={comment}
          onChange={(e) => setComment(e.target.value)}
        />
        <CheckboxField
          label={t('review.reopen')}
          checked={reopen}
          onChange={(e) => setReopen(e.target.checked)}
        />
        <Button type="submit" variant="danger" block busy={reject.isPending}>
          {t('review.confirmReject')}
        </Button>
      </form>
    </Sheet>
  );
}
