import { Navigate, useParams } from 'react-router-dom'
import { themeBySlug } from '../themeConfig'
import { useActiveSurveys, useMyResponse } from '../../surveys/hooks'
import { ThemeActionCard } from '../components/ThemeActionCard'

const ChatIcon = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="h-5 w-5">
    <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z" />
  </svg>
)

const SurveyIcon = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="h-5 w-5">
    <path d="M9 11l3 3L22 4" />
    <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11" />
  </svg>
)

const LearnMoreIcon = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="h-5 w-5">
    <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
    <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
  </svg>
)

export function ThemeDetailPage() {
  const { slug } = useParams<{ slug: string }>()
  const themeEntry = themeBySlug(slug)

  const { data: surveys } = useActiveSurveys()
  const survey = surveys?.find((s) => s.theme === themeEntry?.theme)
  const { data: myResponse, isLoading: loadingResponse } = useMyResponse(survey?.surveyId)

  if (!themeEntry) return <Navigate to="/" replace />

  return (
    <div>
      <h1 className="mb-1 text-2xl font-bold text-ink">{themeEntry.label}</h1>
      {survey?.description && <p className="mb-2 text-sm text-ink-muted">{survey.description}</p>}
      {!loadingResponse && myResponse?.status === 'IN_PROGRESS' && (
        <p className="mb-6 text-sm font-medium text-magenta-600">
          You have a survey in progress - pick up where you left off.
        </p>
      )}

      <div className="mt-6 grid grid-cols-1 gap-6 sm:grid-cols-3">
        <ThemeActionCard
          to="/chat"
          title="Chat"
          description="Ask questions and get personalized guidance."
          icon={<ChatIcon />}
        />
        <ThemeActionCard
          to={`/themes/${slug}/survey`}
          title={myResponse?.status === 'IN_PROGRESS' ? 'Resume survey' : 'Take sub-survey'}
          description={
            survey?.completed ? 'Review or update your answers.' : `Tell us about your ${themeEntry.label.toLowerCase()}.`
          }
          icon={<SurveyIcon />}
        />
        <ThemeActionCard
          to={`/themes/${slug}/learn-more`}
          title="Learn more"
          description="Educational content for this area."
          icon={<LearnMoreIcon />}
        />
      </div>
    </div>
  )
}
