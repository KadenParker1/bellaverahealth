import { useNavigate } from 'react-router-dom'
import { useActiveSurveys, useMyResponse, useSubmitResponse, useSurveyDetail } from '../hooks'
import { SurveyRenderer } from '../renderer/SurveyRenderer'
import { FullPageSpinner } from '../../components/ui/Spinner'
import { ErrorBanner } from '../../components/ui/ErrorBanner'

export function OnboardingPage() {
  const navigate = useNavigate()
  const { data: surveys, isLoading: loadingSurveys, error: surveysError } = useActiveSurveys()
  const onboardingSurvey = surveys?.find((s) => s.code === 'onboarding')

  const { data: surveyDetail, isLoading: loadingDetail } = useSurveyDetail(onboardingSurvey?.surveyId)
  const { data: myResponse, isLoading: loadingResponse } = useMyResponse(onboardingSurvey?.surveyId)
  const submitResponse = useSubmitResponse(onboardingSurvey?.surveyId ?? '')

  if (loadingSurveys || loadingDetail || loadingResponse) return <FullPageSpinner />
  if (surveysError) {
    return (
      <div className="mx-auto max-w-xl px-6 py-16">
        <ErrorBanner error={surveysError} />
      </div>
    )
  }
  if (!surveyDetail) return null

  return (
    <div className="min-h-screen bg-surface-subtle">
      <div className="mx-auto max-w-2xl px-6 py-10 md:px-10">
        <h1 className="mb-1 text-2xl font-bold text-ink">{surveyDetail.title}</h1>
        <p className="mb-8 text-sm text-ink-muted">{surveyDetail.description}</p>
        <SurveyRenderer
          surveyDetail={surveyDetail}
          initialAnswers={myResponse?.answers}
          onSubmit={async (status, answers) => {
            await submitResponse.mutateAsync({ status, answers })
            if (status === 'SUBMITTED') navigate('/', { replace: true })
          }}
        />
      </div>
    </div>
  )
}
