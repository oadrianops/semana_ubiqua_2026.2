import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from '../../shared/ui/toast';
import { api, ApiError } from '../../shared/api/http';
import type { StallDetail } from '../../shared/api/types';
import { Button } from '../../shared/ui/Button';
import { SelectField, TextareaField } from '../../shared/ui/Field';
import { Sheet } from '../../shared/ui/Sheet';

const REASONS = ['PRODUCT_NOT_FOUND', 'PRICE_MISMATCH', 'STALL_CLOSED_OR_MOVED', 'OTHER'] as const;

/**
 * "Informação errada?" (RF-07): motivo, produto opcional e comentário de até 500 caracteres; sem login, com
 * limitação de taxa e campo-armadilha (RN-16).
 */
export function ReportSheet({
  open,
  onOpenChange,
  stall,
}: {
  open: boolean;
  onOpenChange: (o: boolean) => void;
  stall: StallDetail;
}) {
  const { t } = useTranslation('public');
  const [reason, setReason] = useState<(typeof REASONS)[number]>('PRODUCT_NOT_FOUND');
  const [product, setProduct] = useState('');
  const [comment, setComment] = useState('');
  const [website, setWebsite] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await api.post(
        '/reports',
        { stallId: stall.publicId, productSlug: product || null, reason, comment: comment || null, website },
        { retries: 0 },
      );
      toast.success(t('report.sent'));
      onOpenChange(false);
      setComment('');
    } catch (err) {
      setError(err instanceof ApiError ? (err.problem.detail ?? t('report.error')) : t('report.error'));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Sheet
      open={open}
      onOpenChange={onOpenChange}
      title={t('report.title', { code: stall.code })}
      description={t('report.description')}
    >
      <form onSubmit={submit} className="grid gap-4">
        <SelectField
          label={t('report.reason')}
          value={reason}
          onChange={(e) => setReason(e.target.value as (typeof REASONS)[number])}
        >
          {REASONS.map((r) => (
            <option key={r} value={r}>
              {t(`report.reasons.${r}`)}
            </option>
          ))}
        </SelectField>
        <SelectField label={t('report.product')} value={product} onChange={(e) => setProduct(e.target.value)}>
          <option value="">{t('report.wholeStall')}</option>
          {stall.offerings.map((o) => (
            <option key={o.productSlug} value={o.productSlug}>
              {o.productName}
            </option>
          ))}
        </SelectField>
        <TextareaField
          label={t('report.comment')}
          hint={t('report.commentHint')}
          maxLength={500}
          value={comment}
          onChange={(e) => setComment(e.target.value)}
        />
        {/* Campo-armadilha: invisível para pessoas, preenchido por robôs (RN-16). */}
        <div aria-hidden="true" className="absolute -left-[9999px] h-0 overflow-hidden">
          <label>
            {t('report.website')}
            <input
              tabIndex={-1}
              autoComplete="off"
              value={website}
              onChange={(e) => setWebsite(e.target.value)}
              name="website"
            />
          </label>
        </div>
        {error && (
          <p className="text-sm font-semibold text-danger" role="alert">
            {error}
          </p>
        )}
        <Button type="submit" busy={busy} block>
          {t('report.submit')}
        </Button>
      </form>
    </Sheet>
  );
}
