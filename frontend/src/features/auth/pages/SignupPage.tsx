import { zodResolver } from '@hookform/resolvers/zod';
import { MailCheck } from 'lucide-react';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router';
import { z } from 'zod';
import { api, ApiError } from '../../../shared/api/http';
import type { Profile } from '../../../shared/api/types';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { toLanguage } from '../../../shared/i18n/config';
import { useNamespaces } from '../../../shared/i18n/useNamespaces';
import { Button } from '../../../shared/ui/Button';
import { CheckboxField, TextField } from '../../../shared/ui/Field';
import { AuthLayout } from '../AuthLayout';

/**
 * Cadastro de colaborador (RF-08): aceite dos Termos e da Política (versão registrada) e declaração de 18+
 * obrigatórios; senha de 8 a 128 caracteres, sem regras de composição (12.1).
 */
export default function SignupPage() {
  useNamespaces(['account']);
  const { t, i18n } = useTranslation('account');
  const { login } = useAuth();
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);
  const [pendingEmail, setPendingEmail] = useState<string | null>(null);
  const schema = z.object({
    name: z.string().trim().min(2, t('errors.name')).max(120),
    email: z.string().trim().email(t('errors.email')),
    password: z.string().min(8, t('errors.passwordShort')).max(128, t('errors.passwordLong')),
    acceptTerms: z.boolean().refine((v) => v, t('errors.terms')),
    adultDeclaration: z.boolean().refine((v) => v, t('errors.adult')),
  });
  type Values = z.infer<typeof schema>;
  const form = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: { acceptTerms: false, adultDeclaration: false },
  });

  async function onSubmit(values: Values) {
    setError(null);
    try {
      const result = await api.post<{ user: Profile; verificationRequired: boolean }>(
        '/auth/register',
        { ...values, locale: toLanguage(i18n.language) },
        { auth: false, retries: 0 },
      );
      if (result.verificationRequired) {
        setPendingEmail(values.email);
      } else {
        await login(values.email, values.password);
        navigate('/profile?welcome=1', { replace: true });
      }
    } catch (e) {
      setError(e instanceof ApiError ? (e.problem.detail ?? t('signup.failed')) : t('signup.failed'));
    }
  }

  if (pendingEmail) {
    return (
      <AuthLayout title={t('signup.checkTitle')}>
        <p className="flex items-start gap-3 text-ink">
          <MailCheck size={22} aria-hidden="true" className="mt-0.5 shrink-0 text-success" />
          {t('signup.checkBody', { email: pendingEmail })}
        </p>
        <Link to="/login">{t('signup.goLogin')}</Link>
      </AuthLayout>
    );
  }

  return (
    <AuthLayout title={t('signup.title')} lead={t('signup.lead')}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="grid gap-4" noValidate>
        <TextField
          label={t('fields.name')}
          autoComplete="name"
          {...form.register('name')}
          error={form.formState.errors.name?.message}
        />
        <TextField
          label={t('fields.email')}
          type="email"
          autoComplete="email"
          inputMode="email"
          {...form.register('email')}
          error={form.formState.errors.email?.message}
        />
        <TextField
          label={t('fields.password')}
          hint={t('signup.passwordHint')}
          type="password"
          autoComplete="new-password"
          {...form.register('password')}
          error={form.formState.errors.password?.message}
        />
        <CheckboxField
          label={
            <>
              {t('signup.acceptPrefix')} <Link to="/terms">{t('signup.terms')}</Link> {t('signup.and')}{' '}
              <Link to="/privacy">{t('signup.privacy')}</Link>
            </>
          }
          {...form.register('acceptTerms')}
          error={form.formState.errors.acceptTerms?.message}
        />
        <CheckboxField
          label={t('signup.adult')}
          {...form.register('adultDeclaration')}
          error={form.formState.errors.adultDeclaration?.message}
        />
        {error && (
          <p className="font-semibold text-danger" role="alert">
            {error}
          </p>
        )}
        <Button type="submit" size="lg" busy={form.formState.isSubmitting} block>
          {t('signup.submit')}
        </Button>
      </form>
      <p className="text-support">
        {t('signup.hasAccount')} <Link to="/login">{t('signup.login')}</Link>
      </p>
    </AuthLayout>
  );
}
