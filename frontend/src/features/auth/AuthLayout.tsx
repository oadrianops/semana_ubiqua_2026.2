import type { ReactNode } from 'react';

/** Coluna estreita das telas de conta. */
export function AuthLayout({
  title,
  lead,
  children,
}: {
  title: ReactNode;
  lead?: ReactNode;
  children: ReactNode;
}) {
  return (
    <div className="safe-x mx-auto grid max-w-md gap-6 py-8">
      <header className="grid gap-2">
        <h1 className="text-3xl">{title}</h1>
        {lead && <p className="text-support">{lead}</p>}
      </header>
      {children}
    </div>
  );
}
