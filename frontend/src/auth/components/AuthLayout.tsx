import type { ReactNode } from 'react'

/** Shared shell for the pages seen before an account exists - sign in and sign up. */
export function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-[radial-gradient(ellipse_at_top,_var(--color-magenta-100),_var(--color-surface-subtle)_55%)] px-6 py-12">
      <div className="mb-8 text-center">
        <p className="text-3xl font-bold tracking-tight text-magenta-600">Bellavera</p>
        <p className="mt-2 text-sm italic tracking-wide text-ink-muted">
          The beautiful truth about your body
        </p>
      </div>
      {children}
    </div>
  )
}
