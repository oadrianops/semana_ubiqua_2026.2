import i18n, { changeLanguage, initI18n } from './config';

describe('RF-06 inicialização do i18n', () => {
  it('RF-06 RN-19 resolve PT-BR (e não mostra a chave crua) quando o idioma salvo é pt-BR', async () => {
    window.localStorage.setItem('nd.lang', 'pt-BR');
    await initI18n();
    expect(i18n.languages).toContain('pt-BR');
    expect(i18n.t('nav.markets')).toBe('Feiras');
    expect(i18n.t('public:home.title')).not.toBe('home.title');
  });

  it('RF-06 troca para inglês e espanhol com o PT-BR como reserva', async () => {
    await changeLanguage('en');
    expect(i18n.t('nav.markets')).toBe('Markets');
    await changeLanguage('es');
    expect(i18n.languages).toEqual(['es', 'pt-BR']);
  });
});
