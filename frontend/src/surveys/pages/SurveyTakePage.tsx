import { Link, useNavigate, useParams } from 'react-router-dom'
import { themeBySlug } from '../../themes/themeConfig'
import { useActiveSurveys, useMyResponse, useSubmitResponse, useSurveyDetail } from '../hooks'
import { SurveyRenderer } from '../renderer/SurveyRenderer'
import { Spinner } from '../../components/ui/Spinner'
import { ErrorBanner } from '../../components/ui/ErrorBanner'

export function SurveyTakePage() {
  const { slug } = useParams<{ slug: string }>()
  const navigate = useNavigate()
  const themeEntry = themeBySlug(slug)

  const { data: surveys, isLoading: loadingSurveys, error: surveysError } = useActiveSurveys()
  const survey = surveys?.find((s) => s.theme === themeEntry?.theme)

  const { data: surveyDetail, isLoading: loadingDetail } = useSurveyDetail(survey?.surveyId)
  const { data: myResponse, isLoading: loadingResponse } = useMyResponse(survey?.surveyId)
  const submitResponse = useSubmitResponse(survey?.surveyId ?? '')

  if (!themeEntry) {
    return <ErrorBanner error={new Error('Unknown theme.')} />
  }

  if (loadingSurveys || loadingDetail || loadingResponse) {
    return (
      <div className="flex justify-center py-16">
        <Spinner className="h-8 w-8" />
      </div>
    )
  }
  if (surveysError) return <ErrorBanner error={surveysError} />
  if (!surveyDetail) return null

  return (
    <div className="mx-auto max-w-2xl">
      <Link to={`/themes/${slug}`} className="mb-6 inline-block text-sm text-magenta-600 hover:underline">
        &larr; Back to {themeEntry.label}
      </Link>
      <h1 className="mb-1 text-2xl font-bold text-ink">{surveyDetail.title}</h1>
      <p className="mb-8 text-sm text-ink-muted">{surveyDetail.description}</p>
      <SurveyRenderer
        surveyDetail={surveyDetail}
        initialAnswers={myResponse?.answers}
        onSubmit={async (status, answers) => {
          await submitResponse.mutateAsync({ status, answers })
          if (status === 'SUBMITTED') navigate(`/themes/${slug}`)
        }}
      />
    </div>
  )
}
