import { setupI18n } from '../../test/render';
import { describeEntry } from './pages/EarningsPage';

describe('RF-26 extrato', () => {
  it('RF-26 RF-06 descreve o crédito no idioma da pessoa, sem o código interno do tipo', async () => {
    const i18n = await setupI18n();
    const t = i18n.getFixedT('pt-BR', 'collab');
    expect(describeEntry('Tarefa UPDATE_STALL — box 18 (Ver-o-Peso)', t)).toBe(
      'Atualizar box · Box 18 · Ver-o-Peso',
    );
    expect(describeEntry('Repasse PIX E2E-1', t)).toBe('Repasse PIX E2E-1');
  });
});
