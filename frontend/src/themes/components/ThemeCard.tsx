import { Link } from 'react-router-dom'
import type { ThemeConfigEntry } from '../themeConfig'
import type { SurveySummaryDto } from '../../types/api'

export function ThemeCard({ theme, survey }: { theme: ThemeConfigEntry; survey?: SurveySummaryDto }) {
  return (
    <Link
      to={`/themes/${theme.slug}`}
      className="group overflow-hidden rounded-2xl border border-surface-border bg-surface shadow-card transition-transform hover:-translate-y-0.5"
    >
      <div className="aspect-[4/3] overflow-hidden">
        <img
          src={theme.image}
          alt=""
          className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
        />
      </div>
      <div className="p-5">
        <div className="mb-1 flex items-center justify-between">
          <h3 className="text-lg font-semibold text-ink">{theme.label}</h3>
          {survey?.completed && (
            <span className="rounded-full bg-magenta-100 px-2.5 py-0.5 text-xs font-medium text-magenta-700">
              Completed
            </span>
          )}
        </div>
        {survey?.description && <p className="text-sm text-ink-muted">{survey.description}</p>}
      </div>
    </Link>
  )
}
