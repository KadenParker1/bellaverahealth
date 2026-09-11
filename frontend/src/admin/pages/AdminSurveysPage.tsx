import { Link } from 'react-router-dom'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { ErrorBanner } from '../../components/ui/ErrorBanner'
import { Spinner } from '../../components/ui/Spinner'
import type { AdminSurveyDto, SurveyTheme, SurveyVersionStatus } from '../../types/api'
import { useAdminSurveys, useCreateDraft, useUpdateSurvey } from '../hooks'

/** The five themes the schema allows, in the words the app uses for them. */
const THEMES: { value: SurveyTheme; label: string }[] = [
  { value: 'ONBOARDING', label: 'Onboarding' },
  { value: 'EXERCISE', label: 'Exercise' },
  { value: 'NUTRITION', label: 'Nutrition' },
  { value: 'HORMONES', label: 'Hormones' },
  { value: 'PELVIC_FLOOR', label: 'Pelvic floor' },
]

const VERSION_STATUS_LABELS: Record<SurveyVersionStatus, string> = {
  DRAFT: 'draft',
  PUBLISHED: 'live',
  ARCHIVED: 'replaced',
}

const STATUS_STYLES: Record<SurveyVersionStatus, string> = {
  DRAFT: 'bg-amber-100 text-amber-800',
  PUBLISHED: 'bg-emerald-100 text-emerald-800',
  ARCHIVED: 'bg-neutral-200 text-neutral-600',
}

export function AdminSurveysPage() {
  const { data: surveys, isLoading, error } = useAdminSurveys()

  return (
    <div>
      <div className="mb-6">
        <h2 className="text-lg font-semibold text-ink">Surveys</h2>
        <p className="text-sm text-ink-muted">
          Published versions are immutable — editing a live survey means starting a new draft.
          Add content to a survey with "New draft" on its card below; creating a brand-new survey
          isn't available here for now, since it can only ever be for one of the five fixed themes.
        </p>
      </div>

      {error ? <ErrorBanner error={error} /> : null}
      {isLoading && (
        <div className="flex justify-center py-16">
          <Spinner className="h-8 w-8" />
        </div>
      )}

      {surveys && surveys.length === 0 && (
        <Card className="p-10 text-center">
          <p className="text-sm text-ink-muted">No surveys yet.</p>
        </Card>
      )}

      <div className="space-y-4">
        {surveys?.map((survey) => (
          <SurveyCard key={survey.surveyId} survey={survey} />
        ))}
      </div>
    </div>
  )
}

function SurveyCard({ survey }: { survey: AdminSurveyDto }) {
  const createDraft = useCreateDraft()
  const updateSurvey = useUpdateSurvey()
  const draft = survey.versions.find((version) => version.status === 'DRAFT')

  return (
    <Card className="p-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="text-sm font-semibold text-ink">
            {survey.title}
            {!survey.active && (
              <span className="ml-2 rounded-full bg-neutral-200 px-2 py-0.5 text-xs font-medium text-neutral-700">
                Retired
              </span>
            )}
          </p>
          <p className="text-xs text-ink-muted">
            {THEMES.find((option) => option.value === survey.theme)?.label ?? survey.theme}
          </p>
        </div>

        <div className="flex flex-wrap gap-2">
          {draft ? (
            <Link to={`/admin/surveys/${survey.surveyId}/versions/${draft.versionId}`}>
              <Button variant="secondary">Edit draft v{draft.version}</Button>
            </Link>
          ) : (
            <Button
              variant="secondary"
              disabled={createDraft.isPending}
              onClick={() => createDraft.mutate(survey.surveyId)}
            >
              {createDraft.isPending ? 'Creating…' : 'New draft'}
            </Button>
          )}
          <Button
            variant="ghost"
            disabled={updateSurvey.isPending}
            onClick={() =>
              updateSurvey.mutate({ surveyId: survey.surveyId, body: { active: !survey.active } })
            }
          >
            {survey.active ? 'Retire' : 'Restore'}
          </Button>
        </div>
      </div>

      {createDraft.error ? (
        <div className="mt-3">
          <ErrorBanner error={createDraft.error} />
        </div>
      ) : null}

      {/* Retire/Restore can legitimately be refused - restoring a survey whose theme another
          active survey now holds is rejected - so that message has to be visible, or the button
          just appears to do nothing. */}
      {updateSurvey.error ? (
        <div className="mt-3">
          <ErrorBanner error={updateSurvey.error} />
        </div>
      ) : null}

      <ul className="mt-4 flex flex-wrap gap-2 border-t border-surface-border pt-4">
        {survey.versions.map((version) => (
          <li key={version.versionId}>
            <Link
              to={`/admin/surveys/${survey.surveyId}/versions/${version.versionId}`}
              className={`inline-flex items-center gap-2 rounded-full px-3 py-1 text-xs font-medium ${STATUS_STYLES[version.status]}`}
            >
              Version {version.version} · {VERSION_STATUS_LABELS[version.status]} ·{' '}
              {version.questionCount} question{version.questionCount === 1 ? '' : 's'}
            </Link>
          </li>
        ))}
        {survey.versions.length === 0 && (
          <li className="text-xs text-ink-muted">No versions yet.</li>
        )}
      </ul>
    </Card>
  )
}
