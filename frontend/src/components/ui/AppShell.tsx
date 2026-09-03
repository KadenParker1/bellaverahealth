import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../auth/useAuth'
import { Button } from './Button'

export function AppShell({ children }: { children: ReactNode }) {
  const { signOut } = useAuth()

  return (
    <div className="min-h-screen bg-surface-subtle">
      <header className="border-b border-surface-border bg-surface">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-4 md:px-10">
          <Link to="/" className="text-lg font-bold tracking-tight text-magenta-600">
            Bellavera
          </Link>
          <nav className="flex items-center gap-4">
            <Link to="/chat" className="text-sm font-medium text-ink hover:text-magenta-600">
              Chat
            </Link>
            <Button variant="secondary" onClick={() => signOut()}>
              Sign out
            </Button>
          </nav>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-6 py-8 md:px-10">{children}</main>
    </div>
  )
}
