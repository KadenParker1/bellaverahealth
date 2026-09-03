import { Link } from 'react-router-dom'
import type { ReactNode } from 'react'

export function ThemeActionCard({
  to,
  title,
  description,
  icon,
}: {
  to: string
  title: string
  description: string
  icon: ReactNode
}) {
  return (
    <Link
      to={to}
      className="flex flex-col items-start gap-3 rounded-2xl border border-surface-border bg-surface p-6 shadow-card transition-transform hover:-translate-y-0.5"
    >
      <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-magenta-100 text-magenta-600">
        {icon}
      </div>
      <h3 className="text-base font-semibold text-ink">{title}</h3>
      <p className="text-sm text-ink-muted">{description}</p>
    </Link>
  )
}
