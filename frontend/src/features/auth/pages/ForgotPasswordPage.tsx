import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../../shared/api/http';
import { useNamespaces } from '../../../shared/i18n/useNamespaces';
import { Button } from '../../../shared/ui/Button';
import { TextField } from '../../../shared/ui/Field';
import { AuthLayout } from '../AuthLayout';

/** Esqueci a senha (RF-10): a resposta é sempre a mesma, exista ou não a conta. */
export default function ForgotPasswordPage() {
  useNamespaces(['account']);
  const { t } = useTranslation('account');
  const [email, setEmail] = useState('');
  const [sent, setSent] = useState(false);
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    try {
      await api.post('/auth/password/forgot', { email }, { auth: false, retries: 1 });
    } catch {
      // Mesma mensagem em qualquer caso (não revela se o e-mail existe).
    } finally {
      setBusy(false);
      setSent(true);
    }
  }

  return (
    <AuthLayout title={t('forgot.title')} lead={t('forgot.lead')}>
      {sent ? (
        <p className="text-ink" role="status">
          {t('forgot.sent')}
        </p>
      ) : (
        <form onSubmit={submit} className="grid gap-4">
          <TextField
            label={t('fields.email')}
            type="email"
            required
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
          <Button type="submit" size="lg" busy={busy} block>
            {t('forgot.submit')}
          </Button>
        </form>
      )}
    </AuthLayout>
  );
}
