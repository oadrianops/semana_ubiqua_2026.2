import { parsePosition } from './geo';

describe('RF-29 posição do box colada do mapa', () => {
  it('RF-29 aceita latitude e longitude separadas por vírgula, ponto e vírgula ou espaço', () => {
    expect(parsePosition('-1.444643,-48.467934')).toEqual([-1.444643, -48.467934]);
    expect(parsePosition('  -1.444795, -48.468019 ')).toEqual([-1.444795, -48.468019]);
    expect(parsePosition('-1.4448; -48.468')).toEqual([-1.4448, -48.468]);
    expect(parsePosition('-1.4448 -48.468')).toEqual([-1.4448, -48.468]);
  });

  it('RF-29 recusa texto que não é posição ou fora dos limites', () => {
    expect(parsePosition('')).toBeNull();
    expect(parsePosition('Feira da 25')).toBeNull();
    expect(parsePosition('-1.44')).toBeNull();
    expect(parsePosition('95, -48.4')).toBeNull();
    expect(parsePosition('-1.4, -190')).toBeNull();
  });
});
