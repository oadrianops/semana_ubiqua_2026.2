import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useSearchParams } from 'react-router';
import { api } from '../../../shared/api/http';
import { useNamespaces } from '../../../shared/i18n/useNamespaces';
import { LoadingRows } from '../../../shared/ui/States';
import { AuthLayout } from '../AuthLayout';

/** Confirmação de e-mail pelo link de uso único (RF-10). */
export default function VerifyEmailPage() {
  useNamespaces(['account']);
  const { t } = useTranslation('account');
  const [params] = useSearchParams();
  const [state, setState] = useState<'loading' | 'ok' | 'error'>('loading');

  useEffect(() => {
    const token = params.get('token');
    if (!token) {
      setState('error');
      return;
    }
    api
      .post('/auth/verify-email', { token }, { auth: false, retries: 1 })
      .then(() => setState('ok'))
      .catch(() => setState('error'));
  }, [params]);

  return (
    <AuthLayout title={t('verify.title')}>
      {state === 'loading' && <LoadingRows rows={1} />}
      {state === 'ok' && (
        <p className="text-ink">
          {t('verify.ok')} <Link to="/login">{t('verify.login')}</Link>
        </p>
      )}
      {state === 'error' && <p className="font-semibold text-danger">{t('verify.error')}</p>}
    </AuthLayout>
  );
}
