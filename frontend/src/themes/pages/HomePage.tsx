import { useActiveSurveys } from '../../surveys/hooks'
import { THEME_CONFIG } from '../themeConfig'
import { ThemeCard } from '../components/ThemeCard'
import { Spinner } from '../../components/ui/Spinner'
import { ErrorBanner } from '../../components/ui/ErrorBanner'

export function HomePage() {
  const { data: surveys, isLoading, error } = useActiveSurveys()

  return (
    <div>
      <h1 className="mb-1 text-2xl font-bold text-ink">Your health areas</h1>
      <p className="mb-8 text-sm text-ink-muted">
        Pick an area to chat, take a survey, or learn more.
      </p>

      {isLoading && (
        <div className="flex justify-center py-16">
          <Spinner className="h-8 w-8" />
        </div>
      )}
      {error ? <ErrorBanner error={error} /> : null}

      {surveys && (
        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
          {THEME_CONFIG.map((theme) => (
            <ThemeCard
              key={theme.slug}
              theme={theme}
              survey={surveys.find((s) => s.theme === theme.theme)}
            />
          ))}
        </div>
      )}
    </div>
  )
}
