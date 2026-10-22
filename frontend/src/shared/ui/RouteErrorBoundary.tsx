import { Component, type ErrorInfo, type ReactNode } from 'react';
import { ErrorState } from './States';

/**
 * Erro inesperado ao desenhar uma tela (9.4: "error boundary por rota"): mostra o estado de erro dentro do layout,
 * com o cabeçalho funcionando, em vez de derrubar o app inteiro numa tela branca. Quem usa passa `key` com o
 * caminho, para que voltar ou trocar de página recomece do zero.
 */
export class RouteErrorBoundary extends Component<{ children: ReactNode }, { error: unknown }> {
  override state: { error: unknown } = { error: null };

  static getDerivedStateFromError(error: unknown) {
    return { error };
  }

  override componentDidCatch(error: unknown, info: ErrorInfo) {
    console.error('Erro ao desenhar a tela', error, info.componentStack);
  }

  override render() {
    if (this.state.error) {
      return (
        <div className="safe-x mx-auto max-w-[var(--content-max)] py-8">
          <ErrorState error={this.state.error} onRetry={() => window.location.reload()} />
        </div>
      );
    }
    return this.props.children;
  }
}
