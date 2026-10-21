import {
  daysSince,
  formatDayMonth,
  formatDuration,
  formatMoney,
  formatTime,
  relativeFromNow,
  remainingFraction,
} from './format';

describe('RF-06 formatação por idioma (RN-23, RN-24)', () => {
  it('RN-24 moeda sempre BRL, no formato do idioma', () => {
    expect(formatMoney({ amount: '45.00', currency: 'BRL' }, 'pt-BR')).toMatch(/R\$\s?45,00/);
    expect(formatMoney({ amount: '45.00', currency: 'BRL' }, 'en')).toMatch(/R\$45\.00/);
    expect(formatMoney(null, 'pt-BR')).toBeNull();
  });

  it('RN-23 datas no fuso de Belém (UTC−3)', () => {
    // 02:30 UTC do dia 21 é 23:30 do dia 20 em Belém.
    expect(formatDayMonth('2026-10-21T02:30:00Z', 'pt-BR')).toBe('20/10');
    expect(formatTime('2026-10-21T02:30:00Z', 'pt-BR')).toBe('23:30');
  });

  it('RN-12 dias desde a verificação', () => {
    const now = new Date('2026-10-20T15:00:00Z');
    expect(daysSince('2026-10-12T15:00:00Z', now)).toBe(8);
    expect(daysSince(null, now)).toBeNull();
  });

  it('RF-06 tempo relativo nos três idiomas', () => {
    const now = new Date('2026-10-20T15:00:00Z');
    expect(relativeFromNow('2026-10-17T15:00:00Z', 'pt-BR', now)).toContain('3 dias');
    expect(relativeFromNow('2026-10-17T15:00:00Z', 'en', now)).toContain('3 days');
    expect(relativeFromNow('2026-10-17T15:00:00Z', 'es', now)).toContain('3 días');
  });

  it('RN-02 fração restante do prazo e duração', () => {
    const now = new Date('2026-10-20T14:00:00Z');
    expect(remainingFraction('2026-10-20T13:00:00Z', '2026-10-20T15:00:00Z', now)).toBeCloseTo(0.5);
    expect(remainingFraction('2026-10-20T13:00:00Z', '2026-10-20T13:30:00Z', now)).toBe(0);
    expect(formatDuration(90 * 60_000, 'pt-BR')).toBe('1h30');
    expect(formatDuration(12 * 60_000, 'pt-BR')).toBe('12 min');
  });
});
