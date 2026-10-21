import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate, useSearchParams } from 'react-router';
import { z } from 'zod';
import { ApiError } from '../../../shared/api/http';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { useNamespaces } from '../../../shared/i18n/useNamespaces';
import { Button } from '../../../shared/ui/Button';
import { TextField } from '../../../shared/ui/Field';
import { AuthLayout } from '../AuthLayout';

/** Entrar (RF-09): erro sempre genérico; espera progressiva informada sem expor detalhes (12.1). */
export default function LoginPage() {
  useNamespaces(['account']);
  const { t } = useTranslation('account');
  const { login } = useAuth();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [error, setError] = useState<string | null>(null);
  const schema = z.object({
    email: z.string().trim().email(t('errors.email')),
    password: z.string().min(1, t('errors.required')),
  });
  const form = useForm<z.infer<typeof schema>>({ resolver: zodResolver(schema) });

  async function onSubmit(values: z.infer<typeof schema>) {
    setError(null);
    try {
      const user = await login(values.email, values.password);
      const returnTo = params.get('returnTo');
      navigate(
        returnTo && returnTo.startsWith('/')
          ? returnTo
          : user.role === 'COLLABORATOR'
            ? '/collaborate'
            : '/admin',
        { replace: true },
      );
    } catch (e) {
      setError(e instanceof ApiError ? (e.problem.detail ?? t('login.failed')) : t('login.failed'));
    }
  }

  return (
    <AuthLayout title={t('login.title')} lead={t('login.lead')}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="grid gap-4" noValidate>
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
          type="password"
          autoComplete="current-password"
          {...form.register('password')}
          error={form.formState.errors.password?.message}
        />
        {error && (
          <p className="font-semibold text-danger" role="alert">
            {error}
          </p>
        )}
        <Button type="submit" size="lg" busy={form.formState.isSubmitting} block>
          {t('login.submit')}
        </Button>
      </form>
      <div className="grid gap-2 text-base">
        <Link to="/forgot-password" className="inline-flex min-h-11 items-center">
          {t('login.forgot')}
        </Link>
        <p className="text-support">
          {t('login.noAccount')} <Link to="/signup">{t('login.signup')}</Link>
        </p>
      </div>
    </AuthLayout>
  );
}
