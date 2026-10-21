/**
 * Logotipo oficial em curvas (14.6): nunca redigitado com fonte. Versão invertida sobre a faixa Açaí 900.
 * Largura mínima de 96 px.
 */
export function Logo({ reversed = false, width = 132 }: { reversed?: boolean; width?: number }) {
  const src = reversed ? '/brand/svg/nandesk-logo-reversed.svg' : '/brand/svg/nandesk-logo.svg';
  return (
    <img
      src={src}
      width={Math.max(96, width)}
      height={Math.round(Math.max(96, width) / 6.28)}
      alt="NANDESK"
      decoding="async"
    />
  );
}
