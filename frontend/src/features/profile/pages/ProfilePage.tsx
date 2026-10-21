import { useDocumentTitle } from '../../../shared/lib/useDocumentTitle';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Download, LogOut } from 'lucide-react';
import { useEffect, useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { toast } from '../../../shared/ui/toast';
import { api, ApiError } from '../../../shared/api/http';
import type { Profile } from '../../../shared/api/types';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { changeLanguage, LANGUAGE_NAMES, LANGUAGES, toLanguage } from '../../../shared/i18n/config';
import { useNamespaces } from '../../../shared/i18n/useNamespaces';
import { Button } from '../../../shared/ui/Button';
import { CheckboxField, SelectField, TextField } from '../../../shared/ui/Field';
import { Sheet } from '../../../shared/ui/Sheet';
import { useMarkets } from '../../public-api';

const PIX_TYPES = ['CPF', 'EMAIL', 'PHONE', 'RANDOM'] as const;

function errorText(e: unknown, fallback: string): string {
  return e instanceof ApiError ? (e.problem.detail ?? fallback) : fallback;
}

/**
 * Perfil (RF-11, RF-12): dados, idioma, chave PIX (guardada cifrada e exibida mascarada — RN-22), feiras de
 * interesse, exportar dados e pedir a exclusão da conta (LGPD).
 */
export default function ProfilePage() {
  useNamespaces(['account']);
  const { t } = useTranslation('account');
  useDocumentTitle(t('profile.title'));
  const { user, setUser, logout } = useAuth();
  const navigate = useNavigate();
  if (!user) return null;

  return (
    <div className="safe-x mx-auto grid max-w-2xl gap-10 py-6">
      <header className="flex flex-wrap items-end justify-between gap-3">
        <div className="grid gap-1">
          <h1 className="text-3xl">{t('profile.title')}</h1>
          <p className="text-support">{user.email}</p>
        </div>
        <Button
          variant="ghost"
          icon={<LogOut size={18} aria-hidden="true" />}
          onClick={() => void logout().then(() => navigate('/'))}
        >
          {t('profile.logout')}
        </Button>
      </header>
      <PersonalSection user={user} onSaved={setUser} />
      {user.role === 'COLLABORATOR' && <PixSection user={user} onSaved={setUser} />}
      <MarketsSection />
      <PrivacySection onDeleted={() => void logout().then(() => navigate('/'))} />
    </div>
  );
}

function Section({ id, title, children }: { id: string; title: string; children: React.ReactNode }) {
  return (
    <section aria-labelledby={id} className="grid gap-4">
      <h2 id={id} className="border-b-2 border-ink pb-1 text-xl">
        {title}
      </h2>
      {children}
    </section>
  );
}

function PersonalSection({ user, onSaved }: { user: Profile; onSaved: (p: Profile) => void }) {
  const { t, i18n } = useTranslation('account');
  const [name, setName] = useState(user.name);
  const [phone, setPhone] = useState(user.phone ?? '');
  const [locale, setLocale] = useState(user.preferredLocale);
  const save = useMutation({
    mutationFn: () =>
      api.patch<Profile>('/me', { name: name.trim(), phone: phone.trim() || null, locale }, { retries: 0 }),
    onSuccess: (p) => {
      onSaved(p);
      if (p.preferredLocale !== i18n.language) void changeLanguage(toLanguage(p.preferredLocale));
      toast.success(t('profile.saved'));
    },
    onError: (e) => toast.error(errorText(e, t('profile.saveError'))),
  });
  const submit = (e: FormEvent) => {
    e.preventDefault();
    save.mutate();
  };
  return (
    <Section id="personal" title={t('profile.personal')}>
      <form onSubmit={submit} className="grid gap-4">
        <TextField
          label={t('profile.name')}
          value={name}
          maxLength={120}
          required
          autoComplete="name"
          onChange={(e) => setName(e.target.value)}
        />
        <TextField
          label={t('profile.phone')}
          hint={t('profile.optional')}
          type="tel"
          value={phone}
          maxLength={20}
          autoComplete="tel"
          onChange={(e) => setPhone(e.target.value)}
        />
        <SelectField label={t('profile.language')} value={locale} onChange={(e) => setLocale(e.target.value)}>
          {LANGUAGES.map((l) => (
            <option key={l} value={l}>
              {LANGUAGE_NAMES[l]}
            </option>
          ))}
        </SelectField>
        <Button type="submit" className="justify-self-start" busy={save.isPending}>
          {t('profile.save')}
        </Button>
      </form>
    </Section>
  );
}

function PixSection({ user, onSaved }: { user: Profile; onSaved: (p: Profile) => void }) {
  const { t } = useTranslation('account');
  const [type, setType] = useState<(typeof PIX_TYPES)[number]>(
    (user.pixKeyType as (typeof PIX_TYPES)[number]) ?? 'CPF',
  );
  const [key, setKey] = useState('');
  const save = useMutation({
    mutationFn: () => api.put<Profile>('/me/pix-key', { type, key: key.trim() }, { retries: 0 }),
    onSuccess: (p) => {
      onSaved(p);
      setKey('');
      toast.success(t('pix.saved'));
    },
    onError: (e) => toast.error(errorText(e, t('pix.error'))),
  });
  return (
    <Section id="pix" title={t('pix.title')}>
      <p className="text-support">
        {user.pixKeyMasked ? t('pix.current', { key: user.pixKeyMasked }) : t('pix.none')}
      </p>
      <p className="text-sm text-support">{t('pix.why')}</p>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          if (key.trim()) save.mutate();
        }}
        className="grid gap-4 sm:grid-cols-[12rem_1fr]"
      >
        <SelectField
          label={t('pix.type')}
          value={type}
          onChange={(e) => setType(e.target.value as (typeof PIX_TYPES)[number])}
        >
          {PIX_TYPES.map((p) => (
            <option key={p} value={p}>
              {t(`pix.types.${p}`)}
            </option>
          ))}
        </SelectField>
        <TextField
          label={t('pix.key')}
          value={key}
          maxLength={140}
          autoComplete="off"
          onChange={(e) => setKey(e.target.value)}
        />
        <Button type="submit" className="justify-self-start" busy={save.isPending} disabled={!key.trim()}>
          {user.pixKeyMasked ? t('pix.replace') : t('pix.save')}
        </Button>
      </form>
    </Section>
  );
}

function MarketsSection() {
  const { t } = useTranslation('account');
  const markets = useMarkets();
  const client = useQueryClient();
  const subs = useQuery({
    queryKey: ['me', 'market-subscriptions'],
    queryFn: () => api.get<{ markets: string[] }>('/me/market-subscriptions'),
    networkMode: 'online',
  });
  const [selected, setSelected] = useState<string[]>([]);
  useEffect(() => {
    if (subs.data) setSelected(subs.data.markets);
  }, [subs.data]);
  const save = useMutation({
    mutationFn: (list: string[]) =>
      api.put<{ markets: string[] }>('/me/market-subscriptions', { markets: list }, { retries: 0 }),
    onSuccess: () => {
      void client.invalidateQueries({ queryKey: ['me', 'market-subscriptions'] });
      toast.success(t('markets.saved'));
    },
    onError: (e) => toast.error(errorText(e, t('markets.error'))),
  });
  const toggle = (slug: string, on: boolean) =>
    setSelected((s) => (on ? [...s, slug] : s.filter((x) => x !== slug)));
  return (
    <Section id="markets" title={t('markets.title')}>
      <p className="text-sm text-support">{t('markets.body')}</p>
      <div className="grid">
        {markets.data?.map((m) => (
          <CheckboxField
            key={m.slug}
            label={m.name}
            checked={selected.includes(m.slug)}
            onChange={(e) => toggle(m.slug, e.target.checked)}
          />
        ))}
      </div>
      <Button
        className="justify-self-start"
        busy={save.isPending}
        disabled={!subs.data}
        onClick={() => save.mutate(selected)}
      >
        {t('markets.save')}
      </Button>
    </Section>
  );
}

function PrivacySection({ onDeleted }: { onDeleted: () => void }) {
  const { t } = useTranslation('account');
  const [exporting, setExporting] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [typed, setTyped] = useState('');
  const word = t('privacy.confirmWord');
  const remove = useMutation({
    mutationFn: () => api.post('/me/deletion-request', undefined, { retries: 0 }),
    onSuccess: () => {
      toast.success(t('privacy.deleted'));
      setConfirmOpen(false);
      onDeleted();
    },
    onError: (e) => toast.error(errorText(e, t('privacy.deleteError'))),
  });

  async function exportData() {
    setExporting(true);
    try {
      const data = await api.get<unknown>('/me/export', { retries: 0 });
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'nandesk-meus-dados.json';
      a.click();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (e) {
      toast.error(errorText(e, t('privacy.exportError')));
    } finally {
      setExporting(false);
    }
  }

  return (
    <Section id="privacy" title={t('privacy.title')}>
      <p className="text-sm text-support">{t('privacy.body')}</p>
      <div className="flex flex-wrap gap-3">
        <Button
          variant="secondary"
          icon={<Download size={18} aria-hidden="true" />}
          busy={exporting}
          onClick={() => void exportData()}
        >
          {t('privacy.export')}
        </Button>
        <Button variant="danger" onClick={() => setConfirmOpen(true)}>
          {t('privacy.delete')}
        </Button>
      </div>
      <Sheet
        open={confirmOpen}
        onOpenChange={setConfirmOpen}
        title={t('privacy.confirmTitle')}
        description={t('privacy.confirmBody')}
      >
        <form
          className="grid gap-4"
          onSubmit={(e) => {
            e.preventDefault();
            if (typed.trim().toUpperCase() === word.toUpperCase()) remove.mutate();
          }}
        >
          <TextField
            label={t('privacy.confirmLabel', { word })}
            value={typed}
            autoComplete="off"
            onChange={(e) => setTyped(e.target.value)}
          />
          <Button
            type="submit"
            variant="danger"
            block
            busy={remove.isPending}
            disabled={typed.trim().toUpperCase() !== word.toUpperCase()}
          >
            {t('privacy.confirmAction')}
          </Button>
        </form>
      </Sheet>
    </Section>
  );
}
