import { PersistQueryClientProvider } from '@tanstack/react-query-persist-client';
import { Suspense } from 'react';
import { BrowserRouter } from 'react-router';
import { Toaster } from '../shared/ui/toast';
import { AuthProvider } from '../shared/auth/AuthProvider';
import { LoadingRows } from '../shared/ui/States';
import { persister, queryClient, shouldPersist } from './queryClient';
import { AppRoutes } from './routes';

export function App() {
  return (
    <PersistQueryClientProvider
      client={queryClient}
      persistOptions={{
        persister,
        maxAge: 7 * 24 * 60 * 60_000,
        // Cada versão publicada descarta as respostas guardadas pela anterior: o formato da API pode ter mudado.
        buster: __BUILD_ID__,
        dehydrateOptions: { shouldDehydrateQuery: shouldPersist },
      }}
    >
      <AuthProvider>
        <BrowserRouter>
          <Suspense
            fallback={
              <div className="safe-x mx-auto max-w-[var(--content-max)] pt-24">
                <LoadingRows />
              </div>
            }
          >
            <AppRoutes />
          </Suspense>
        </BrowserRouter>
      </AuthProvider>
      <Toaster />
    </PersistQueryClientProvider>
  );
}
