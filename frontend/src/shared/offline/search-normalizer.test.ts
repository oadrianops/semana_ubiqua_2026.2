import contract from '../../../../contracts/search-normalization-cases.json';
import { normalize, score, tokens } from './search-normalizer';

describe('RF-36 busca local: normalização igual à do servidor', () => {
  it.each(contract.cases)(
    'RF-02 normaliza "$input" como "$expected" (contrato compartilhado)',
    ({ input, expected }) => {
      expect(normalize(input)).toBe(expected);
    },
  );

  it('RF-02 "acai" encontra "Açaí"', () => {
    expect(normalize('acai')).toBe(normalize('Açaí'));
  });

  it('RF-02 divide em palavras', () => {
    expect(tokens('Castanha-do-Pará')).toEqual(['castanha', 'do', 'para']);
    expect(tokens('')).toEqual([]);
  });

  it('RF-02 pontua como o servidor', () => {
    expect(score('jambu', 'jambu')).toBe(100);
    expect(score('castanha do para', 'castanha')).toBe(80);
    expect(score('oleo de copaiba', 'copa')).toBe(60);
    expect(score('pato no tucupi', 'ucup')).toBe(40);
    expect(score('castanha do para', 'para castanha')).toBe(40);
    expect(score('brazil nut', 'brazil nut', true)).toBe(110);
    expect(score('tucupi', 'acai')).toBe(0);
    expect(score('tucupi', '')).toBe(0);
  });
});
