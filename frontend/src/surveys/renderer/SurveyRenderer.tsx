import { useMemo, useState } from 'react'
import type { AnswerDto, QuestionDto, ResponseStatus, SurveyDetailDto } from '../../types/api'
import { initialAnswersFrom, isAnswered, toAnswerRequest, type AnswersState } from '../types'
import { isVisible } from './displayRuleEngine'
import { QUESTION_COMPONENTS } from './questionRegistry'
import { Button } from '../../components/ui/Button'
import { ErrorBanner } from '../../components/ui/ErrorBanner'

interface SurveyRendererProps {
  surveyDetail: SurveyDetailDto
  initialAnswers?: AnswerDto[]
  onSubmit: (status: ResponseStatus, answers: ReturnType<typeof toAnswerRequest>[]) => Promise<void>
}

export function SurveyRenderer({ surveyDetail, initialAnswers, onSubmit }: SurveyRendererProps) {
  const [answers, setAnswers] = useState<AnswersState>(() => initialAnswersFrom(initialAnswers))
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [submitError, setSubmitError] = useState<unknown>(null)
  const [saving, setSaving] = useState<ResponseStatus | null>(null)

  const allQuestions = useMemo(
    () => surveyDetail.sections.flatMap((section) => section.questions),
    [surveyDetail],
  )

  function visibleRequiredQuestions(): QuestionDto[] {
    return allQuestions.filter((q) => q.required && isVisible(q.displayRule, answers))
  }

  function buildAnswerRequests() {
    return allQuestions
      .filter((q) => isAnswered(answers[q.code]))
      .map((q) => toAnswerRequest(q, answers[q.code]))
  }

  async function handleSubmit(status: ResponseStatus) {
    setSubmitError(null)

    if (status === 'SUBMITTED') {
      const missing = visibleRequiredQuestions().filter((q) => !isAnswered(answers[q.code]))
      if (missing.length > 0) {
        setErrors(Object.fromEntries(missing.map((q) => [q.code, 'This question is required.'])))
        return
      }
    }
    setErrors({})
    setSaving(status)
    try {
      await onSubmit(status, buildAnswerRequests())
    } catch (err) {
      setSubmitError(err)
    } finally {
      setSaving(null)
    }
  }

  return (
    <div className="space-y-8">
      {surveyDetail.sections.map((section) => {
        const visibleQuestions = section.questions.filter((q) => isVisible(q.displayRule, answers))
        if (visibleQuestions.length === 0) return null

        return (
          <div key={section.code}>
            <h2 className="mb-1 text-lg font-semibold text-ink">{section.title}</h2>
            {section.description && (
              <p className="mb-4 text-sm text-ink-muted">{section.description}</p>
            )}
            <div className="space-y-6">
              {visibleQuestions.map((question) => {
                const QuestionComponent = QUESTION_COMPONENTS[question.type]
                return (
                  <QuestionComponent
                    key={question.code}
                    question={question}
                    value={answers[question.code]}
                    onChange={(value) => setAnswers((prev) => ({ ...prev, [question.code]: value }))}
                    error={errors[question.code]}
                  />
                )
              })}
            </div>
          </div>
        )
      })}

      {submitError ? <ErrorBanner error={submitError} /> : null}

      <div className="flex justify-end gap-3 border-t border-surface-border pt-6">
        <Button
          type="button"
          variant="secondary"
          disabled={saving !== null}
          onClick={() => handleSubmit('IN_PROGRESS')}
        >
          {saving === 'IN_PROGRESS' ? 'Saving...' : 'Save & exit'}
        </Button>
        <Button type="button" disabled={saving !== null} onClick={() => handleSubmit('SUBMITTED')}>
          {saving === 'SUBMITTED' ? 'Submitting...' : 'Submit'}
        </Button>
      </div>
    </div>
  )
}
