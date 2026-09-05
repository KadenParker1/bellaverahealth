import type { ReactNode } from 'react'
import { Link, NavLink } from 'react-router-dom'
import { useAuth } from '../../auth/useAuth'
import { useMe } from '../../profile/hooks'
import { useCart } from '../../store/CartContext'

const NAV_ITEMS = [
  { label: 'Home', to: '/' },
  { label: 'About', to: '/about' },
  { label: 'Contact', to: '/contact' },
  { label: 'Blog', to: '/blog' },
  { label: 'Store', to: '/store' },
  { label: 'My Account', to: '/account' },
] as const

export function AppShell({ children }: { children: ReactNode }) {
  const { signOut } = useAuth()
  const { data: me } = useMe()
  const { itemCount } = useCart()

  return (
    <div className="min-h-screen bg-surface-subtle">
      <header>
        <div className="border-b border-surface-border bg-surface">
          <div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-4 md:px-10">
            <Link to="/" className="text-lg font-bold tracking-tight text-magenta-600">
              Bellavera
            </Link>
            <div className="flex items-center gap-4">
              <Link to="/chat" className="text-sm font-medium text-ink hover:text-magenta-600">
                Chat
              </Link>
              {me?.role === 'ADMIN' && (
                <Link to="/admin" className="text-sm font-medium text-ink hover:text-magenta-600">
                  Admin
                </Link>
              )}
              <button
                type="button"
                onClick={() => signOut()}
                className="text-sm font-medium text-ink-muted hover:text-magenta-600"
              >
                Sign out
              </button>
            </div>
          </div>
        </div>

        {/* The dark bar: white uppercase links divided by hairline white rules. */}
        <nav className="bg-ink">
          <ul className="mx-auto flex max-w-6xl flex-wrap items-stretch px-6 md:px-10">
            {NAV_ITEMS.map((item, index) => (
              <li key={item.to} className={index > 0 ? 'border-l border-white/40' : undefined}>
                <NavLink
                  to={item.to}
                  end={item.to === '/'}
                  className={({ isActive }) =>
                    [
                      'flex items-center gap-2 px-4 py-3 text-xs font-semibold uppercase tracking-[0.14em] transition-colors sm:px-6',
                      isActive ? 'bg-white/15 text-white' : 'text-white/85 hover:bg-white/10 hover:text-white',
                    ].join(' ')
                  }
                >
                  {item.label}
                  {item.to === '/store' && itemCount > 0 && (
                    <span className="rounded-full bg-magenta-500 px-1.5 py-0.5 text-[10px] leading-none text-white">
                      {itemCount}
                    </span>
                  )}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>
      </header>

      <main className="mx-auto max-w-6xl px-6 py-8 md:px-10">{children}</main>
    </div>
  )
}
