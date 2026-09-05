import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { ErrorBanner } from '../../components/ui/ErrorBanner'
import { Spinner } from '../../components/ui/Spinner'
import type { AdminSurveyDto, SurveyTheme, SurveyVersionStatus } from '../../types/api'
import { useAdminSurveys, useCreateDraft, useCreateSurvey, useUpdateSurvey } from '../hooks'
import { slugify } from '../surveyEditorState'

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
  const [creating, setCreating] = useState(false)

  return (
    <div>
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-ink">Surveys</h2>
          <p className="text-sm text-ink-muted">
            Published versions are immutable — editing a live survey means starting a new draft.
          </p>
        </div>
        <Button onClick={() => setCreating((current) => !current)}>
          {creating ? 'Close' : 'New survey'}
        </Button>
      </div>

      {creating && <CreateSurveyCard onDone={() => setCreating(false)} />}

      {error ? <ErrorBanner error={error} /> : null}
      {isLoading && (
        <div className="flex justify-center py-16">
          <Spinner className="h-8 w-8" />
        </div>
      )}

      {surveys && surveys.length === 0 && !creating && (
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

function CreateSurveyCard({ onDone }: { onDone: () => void }) {
  const createSurvey = useCreateSurvey()
  const [theme, setTheme] = useState<SurveyTheme>('EXERCISE')
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')

  // The code is an identifier, not a decision: derived from the title, never typed. It is
  // permanent once created, which is why it is shown rather than hidden entirely.
  const code = slugify(title)

  const field =
    'w-full rounded-lg border border-surface-border bg-white px-3 py-2 text-sm text-ink outline-none focus:border-magenta-500'

  const onSubmit = (event: FormEvent) => {
    event.preventDefault()
    if (!code) return
    createSurvey.mutate(
      { code, theme, title: title.trim(), description: description.trim() || undefined },
      { onSuccess: onDone },
    )
  }

  return (
    <Card className="mb-6 p-6">
      <h3 className="mb-1 text-base font-semibold text-ink">New survey</h3>
      <p className="mb-4 text-xs text-ink-muted">
        Creates the survey and an empty first draft for you to fill in. Nothing is visible to
        anyone until you publish it.
      </p>
      <form onSubmit={onSubmit} className="space-y-4">
        {createSurvey.error ? <ErrorBanner error={createSurvey.error} /> : null}
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <label className="block text-sm">
            <span className="mb-1 block text-xs font-medium text-ink-muted">Name</span>
            <input
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              required
              placeholder="Sleep quality"
              className={field}
            />
            {title.trim() !== '' && (
              <span className="mt-1 block text-xs text-ink-muted">
                {code ? (
                  <>
                    Identifier: <code className="text-ink">{code}</code> — set automatically and
                    permanent, so answers stay matched to this survey as you revise it.
                  </>
                ) : (
                  <span className="text-red-600">
                    Please include some letters or numbers in the name.
                  </span>
                )}
              </span>
            )}
          </label>
          <label className="block text-sm">
            <span className="mb-1 block text-xs font-medium text-ink-muted">Health area</span>
            <select
              value={theme}
              onChange={(event) => setTheme(event.target.value as SurveyTheme)}
              className={field}
            >
              {THEMES.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
        </div>
        <label className="block text-sm">
          <span className="mb-1 block text-xs font-medium text-ink-muted">
            Short description (optional)
          </span>
          <textarea
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            rows={2}
            className={field}
          />
        </label>
        <div className="flex gap-3">
          <Button type="submit" disabled={createSurvey.isPending || !code}>
            {createSurvey.isPending ? 'Creating…' : 'Create survey'}
          </Button>
          <Button type="button" variant="secondary" onClick={onDone}>
            Cancel
          </Button>
        </div>
      </form>
    </Card>
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
