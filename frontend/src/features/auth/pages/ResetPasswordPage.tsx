import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useSearchParams } from 'react-router';
import { api, ApiError } from '../../../shared/api/http';
import { useNamespaces } from '../../../shared/i18n/useNamespaces';
import { Button } from '../../../shared/ui/Button';
import { TextField } from '../../../shared/ui/Field';
import { AuthLayout } from '../AuthLayout';

/** Redefinir senha (RF-10): link de 30 min, de uso único; encerra todas as sessões. */
export default function ResetPasswordPage() {
  useNamespaces(['account']);
  const { t } = useTranslation('account');
  const [params] = useSearchParams();
  const [password, setPassword] = useState('');
  const [state, setState] = useState<'form' | 'ok'>('form');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (password.length < 8) {
      setError(t('errors.passwordShort'));
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await api.post(
        '/auth/password/reset',
        { token: params.get('token') ?? '', password },
        { auth: false, retries: 0 },
      );
      setState('ok');
    } catch (err) {
      setError(
        err instanceof ApiError && err.code === 'VALIDATION_ERROR' ? t('reset.invalid') : t('reset.failed'),
      );
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthLayout title={t('reset.title')}>
      {state === 'ok' ? (
        <p className="text-ink">
          {t('reset.ok')} <Link to="/login">{t('verify.login')}</Link>
        </p>
      ) : (
        <form onSubmit={submit} className="grid gap-4">
          <TextField
            label={t('reset.newPassword')}
            hint={t('signup.passwordHint')}
            type="password"
            autoComplete="new-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            error={error}
          />
          <Button type="submit" size="lg" busy={busy} block>
            {t('reset.submit')}
          </Button>
        </form>
      )}
    </AuthLayout>
  );
}
