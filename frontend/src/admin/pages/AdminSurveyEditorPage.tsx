import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { ErrorBanner } from '../../components/ui/ErrorBanner'
import { Spinner } from '../../components/ui/Spinner'
import type { AdminSurveyVersionDto, QuestionType } from '../../types/api'
import { useAdminVersion, useDeleteDraft, usePublishVersion, useSaveVersion } from '../hooks'
import { QUESTION_TYPES, QUESTION_TYPE_LABELS, isChoiceType } from '../questionTypes'
import {
  assignCodes,
  move,
  newCondition,
  newOption,
  newQuestion,
  newSection,
  toEditorSections,
  toSectionDtos,
  type AssignedCodes,
  type EditorQuestion,
  type EditorSection,
} from '../surveyEditorState'

const FIELD =
  'w-full rounded-lg border border-surface-border bg-white px-3 py-2 text-sm text-ink outline-none focus:border-magenta-500 disabled:bg-surface-subtle disabled:text-ink-muted'
const LABEL = 'mb-1 block text-xs font-medium text-ink-muted'
const HINT = 'mt-1 block text-xs text-ink-muted'

/** One entry per question in the version, for the "only show this if…" question picker. */
interface CatalogEntry {
  id: string
  code: string
  prompt: string
  type: QuestionType
  options: { code: string; label: string }[]
}

export function AdminSurveyEditorPage() {
  const { surveyId, versionId } = useParams<{ surveyId: string; versionId: string }>()
  const { data: version, isLoading, error } = useAdminVersion(surveyId, versionId)

  if (isLoading) {
    return (
      <div className="flex justify-center py-16">
        <Spinner className="h-8 w-8" />
      </div>
    )
  }
  if (error) return <ErrorBanner error={error} />
  if (!version || !surveyId || !versionId) return null

  return (
    <VersionEditor key={version.versionId} surveyId={surveyId} versionId={versionId} version={version} />
  )
}

function VersionEditor({
  surveyId,
  versionId,
  version,
}: {
  surveyId: string
  versionId: string
  version: AdminSurveyVersionDto
}) {
  const navigate = useNavigate()
  const save = useSaveVersion(surveyId, versionId)
  const publish = usePublishVersion(surveyId, versionId)
  const deleteDraft = useDeleteDraft()

  const [sections, setSections] = useState<EditorSection[]>(() => toEditorSections(version))
  const [notes, setNotes] = useState(version.notes ?? '')

  const readOnly = version.status !== 'DRAFT'
  const codes = assignCodes(sections)
  const catalog: CatalogEntry[] = sections.flatMap((section) =>
    section.questions.map((question) => ({
      id: question.id,
      code: codes.questions.get(question.id) as string,
      prompt: question.prompt || '(untitled question)',
      type: question.type,
      options: question.options.map((option) => ({
        code: codes.options.get(option.id) as string,
        label: option.label || '(unnamed choice)',
      })),
    })),
  )

  const questionCount = sections.reduce((total, section) => total + section.questions.length, 0)

  const updateSection = (index: number, patch: Partial<EditorSection>) =>
    setSections((current) => current.map((section, i) => (i === index ? { ...section, ...patch } : section)))

  return (
    <div>
      <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
        <div>
          <Link to="/admin/surveys" className="text-xs font-medium text-magenta-600 hover:underline">
            ← All surveys
          </Link>
          <h2 className="mt-1 text-lg font-semibold text-ink">
            Version {version.version} · {version.status.toLowerCase()}
          </h2>
          <p className="text-sm text-ink-muted">
            {readOnly
              ? 'This version is live or retired, so it can’t be changed. Start a new draft to make edits.'
              : 'This is a draft. Nothing you change here reaches anyone until you publish it.'}
          </p>
        </div>

        {!readOnly && (
          <div className="flex flex-wrap gap-2">
            <Button
              variant="secondary"
              disabled={save.isPending}
              onClick={() => save.mutate({ notes: notes.trim() || undefined, sections: toSectionDtos(sections) })}
            >
              {save.isPending ? 'Saving…' : 'Save draft'}
            </Button>
            <Button disabled={publish.isPending || questionCount === 0} onClick={() => publish.mutate()}>
              {publish.isPending ? 'Publishing…' : 'Publish'}
            </Button>
            <Button
              variant="ghost"
              disabled={deleteDraft.isPending}
              onClick={() =>
                deleteDraft.mutate({ surveyId, versionId }, { onSuccess: () => navigate('/admin/surveys') })
              }
            >
              Delete draft
            </Button>
          </div>
        )}
      </div>

      <div className="mb-6 space-y-3">
        {save.error ? <ErrorBanner error={save.error} /> : null}
        {publish.error ? <ErrorBanner error={publish.error} /> : null}
        {deleteDraft.error ? <ErrorBanner error={deleteDraft.error} /> : null}
        {save.isSuccess && !save.isPending && !save.error ? (
          <p className="rounded-lg bg-emerald-50 px-4 py-2 text-sm text-emerald-800">
            Draft saved. It isn’t live until you publish.
          </p>
        ) : null}
      </div>

      {!readOnly && (
        <Card className="mb-6 p-6">
          <label className="block text-sm">
            <span className={LABEL}>Note to yourself about this version (optional)</span>
            <input value={notes} onChange={(event) => setNotes(event.target.value)} className={FIELD} />
            <span className={HINT}>Only ever shown here. People taking the survey never see it.</span>
          </label>
        </Card>
      )}

      <div className="space-y-6">
        {sections.map((section, sectionIndex) => (
          <SectionEditor
            key={section.id}
            section={section}
            index={sectionIndex}
            total={sections.length}
            readOnly={readOnly}
            codes={codes}
            catalog={catalog}
            onChange={(patch) => updateSection(sectionIndex, patch)}
            onMove={(delta) => setSections((current) => move(current, sectionIndex, sectionIndex + delta))}
            onRemove={() => setSections((current) => current.filter((_, i) => i !== sectionIndex))}
          />
        ))}
      </div>

      {sections.length === 0 && (
        <Card className="p-10 text-center">
          <p className="mb-1 text-sm text-ink">This survey is empty.</p>
          <p className="text-sm text-ink-muted">
            Add a section to group your questions — most surveys need only one.
          </p>
        </Card>
      )}

      {!readOnly && (
        <Button
          variant="secondary"
          className="mt-6"
          onClick={() => setSections((current) => [...current, newSection(current.length)])}
        >
          Add section
        </Button>
      )}
    </div>
  )
}

function SectionEditor({
  section,
  index,
  total,
  readOnly,
  codes,
  catalog,
  onChange,
  onMove,
  onRemove,
}: {
  section: EditorSection
  index: number
  total: number
  readOnly: boolean
  codes: AssignedCodes
  catalog: CatalogEntry[]
  onChange: (patch: Partial<EditorSection>) => void
  onMove: (delta: number) => void
  onRemove: () => void
}) {
  const updateQuestion = (questionIndex: number, patch: Partial<EditorQuestion>) =>
    onChange({
      questions: section.questions.map((question, i) =>
        i === questionIndex ? { ...question, ...patch } : question,
      ),
    })

  return (
    <Card className="p-6">
      <div className="mb-4 flex items-start justify-between gap-4">
        <h3 className="text-sm font-semibold text-ink">Section {index + 1}</h3>
        {!readOnly && (
          <ReorderControls
            index={index}
            total={total}
            onMove={onMove}
            onRemove={onRemove}
            removeLabel="Remove section"
          />
        )}
      </div>

      <label className="block text-sm">
        <span className={LABEL}>Section heading</span>
        <input
          value={section.title}
          disabled={readOnly}
          onChange={(event) => onChange({ title: event.target.value })}
          className={FIELD}
        />
      </label>
      <label className="mt-4 block text-sm">
        <span className={LABEL}>Intro text (optional)</span>
        <input
          value={section.description}
          disabled={readOnly}
          onChange={(event) => onChange({ description: event.target.value })}
          className={FIELD}
        />
      </label>

      <div className="mt-6 space-y-4 border-t border-surface-border pt-4">
        {section.questions.map((question, questionIndex) => (
          <QuestionEditor
            key={question.id}
            question={question}
            index={questionIndex}
            total={section.questions.length}
            readOnly={readOnly}
            code={codes.questions.get(question.id) ?? ''}
            codes={codes}
            catalog={catalog}
            onChange={(patch) => updateQuestion(questionIndex, patch)}
            onMove={(delta) =>
              onChange({ questions: move(section.questions, questionIndex, questionIndex + delta) })
            }
            onRemove={() =>
              onChange({ questions: section.questions.filter((_, i) => i !== questionIndex) })
            }
          />
        ))}

        {!readOnly && (
          <Button
            variant="ghost"
            onClick={() => onChange({ questions: [...section.questions, newQuestion()] })}
          >
            Add question
          </Button>
        )}
      </div>
    </Card>
  )
}

function QuestionEditor({
  question,
  index,
  total,
  readOnly,
  code,
  codes,
  catalog,
  onChange,
  onMove,
  onRemove,
}: {
  question: EditorQuestion
  index: number
  total: number
  readOnly: boolean
  code: string
  codes: AssignedCodes
  catalog: CatalogEntry[]
  onChange: (patch: Partial<EditorQuestion>) => void
  onMove: (delta: number) => void
  onRemove: () => void
}) {
  const others = catalog.filter((entry) => entry.id !== question.id)

  return (
    <div className="rounded-xl border border-surface-border bg-surface-subtle p-4">
      <div className="mb-3 flex items-start justify-between gap-4">
        <p className="text-xs font-semibold uppercase tracking-wide text-ink-muted">
          Question {index + 1}
        </p>
        {!readOnly && (
          <ReorderControls index={index} total={total} onMove={onMove} onRemove={onRemove} removeLabel="Remove" />
        )}
      </div>

      <label className="block text-sm">
        <span className={LABEL}>What are you asking?</span>
        <input
          value={question.prompt}
          disabled={readOnly}
          placeholder="How often do you exercise?"
          onChange={(event) => onChange({ prompt: event.target.value })}
          className={FIELD}
        />
      </label>

      <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
        <label className="block text-sm">
          <span className={LABEL}>How should they answer?</span>
          <select
            value={question.type}
            disabled={readOnly}
            onChange={(event) => {
              const type = event.target.value as QuestionType
              onChange({
                type,
                options:
                  isChoiceType(type) && question.options.length === 0
                    ? [newOption(), newOption()]
                    : question.options,
              })
            }}
            className={FIELD}
          >
            {QUESTION_TYPES.map((type) => (
              <option key={type} value={type}>
                {QUESTION_TYPE_LABELS[type]}
              </option>
            ))}
          </select>
        </label>
        <label className="flex items-center gap-2 self-end pb-2 text-sm text-ink">
          <input
            type="checkbox"
            checked={question.required}
            disabled={readOnly}
            onChange={(event) => onChange({ required: event.target.checked })}
            className="h-4 w-4 rounded border-surface-border"
          />
          They must answer this
        </label>
      </div>

      <label className="mt-4 block text-sm">
        <span className={LABEL}>Hint below the question (optional)</span>
        <input
          value={question.helpText}
          disabled={readOnly}
          onChange={(event) => onChange({ helpText: event.target.value })}
          className={FIELD}
        />
      </label>

      <TypeSettings question={question} readOnly={readOnly} onChange={onChange} />

      {isChoiceType(question.type) && (
        <ChoicesEditor question={question} readOnly={readOnly} onChange={onChange} />
      )}

      <ConditionsEditor
        question={question}
        others={others}
        readOnly={readOnly}
        onChange={onChange}
      />

      <AdvancedPanel question={question} code={code} codes={codes} />
    </div>
  )
}

/** The handful of settings the survey renderer actually reads, per answer type. */
function TypeSettings({
  question,
  readOnly,
  onChange,
}: {
  question: EditorQuestion
  readOnly: boolean
  onChange: (patch: Partial<EditorQuestion>) => void
}) {
  if (question.type === 'SCALE') {
    return (
      <div className="mt-4 rounded-lg border border-surface-border bg-white p-3">
        <p className={LABEL}>Scale</p>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          <label className="block text-sm">
            <span className={LABEL}>From</span>
            <input
              type="number"
              value={question.min}
              disabled={readOnly}
              onChange={(event) => onChange({ min: event.target.value })}
              className={FIELD}
            />
          </label>
          <label className="block text-sm">
            <span className={LABEL}>To</span>
            <input
              type="number"
              value={question.max}
              disabled={readOnly}
              onChange={(event) => onChange({ max: event.target.value })}
              className={FIELD}
            />
          </label>
          <label className="block text-sm">
            <span className={LABEL}>Label for the low end</span>
            <input
              value={question.scaleMinLabel}
              disabled={readOnly}
              placeholder="Not at all"
              onChange={(event) => onChange({ scaleMinLabel: event.target.value })}
              className={FIELD}
            />
          </label>
          <label className="block text-sm">
            <span className={LABEL}>Label for the high end</span>
            <input
              value={question.scaleMaxLabel}
              disabled={readOnly}
              placeholder="Very much"
              onChange={(event) => onChange({ scaleMaxLabel: event.target.value })}
              className={FIELD}
            />
          </label>
        </div>
      </div>
    )
  }

  if (question.type === 'NUMBER') {
    return (
      <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-3">
        <label className="block text-sm">
          <span className={LABEL}>Smallest allowed (optional)</span>
          <input
            type="number"
            value={question.min}
            disabled={readOnly}
            onChange={(event) => onChange({ min: event.target.value })}
            className={FIELD}
          />
        </label>
        <label className="block text-sm">
          <span className={LABEL}>Largest allowed (optional)</span>
          <input
            type="number"
            value={question.max}
            disabled={readOnly}
            onChange={(event) => onChange({ max: event.target.value })}
            className={FIELD}
          />
        </label>
        <label className="block text-sm">
          <span className={LABEL}>Unit (optional)</span>
          <input
            value={question.unit}
            disabled={readOnly}
            placeholder="kg"
            onChange={(event) => onChange({ unit: event.target.value })}
            className={FIELD}
          />
        </label>
      </div>
    )
  }

  if (question.type === 'TEXT' || question.type === 'LONG_TEXT') {
    return (
      <label className="mt-4 block text-sm sm:w-1/3">
        <span className={LABEL}>Character limit (optional)</span>
        <input
          type="number"
          value={question.maxLength}
          disabled={readOnly}
          placeholder="300"
          onChange={(event) => onChange({ maxLength: event.target.value })}
          className={FIELD}
        />
      </label>
    )
  }

  return null
}

function ChoicesEditor({
  question,
  readOnly,
  onChange,
}: {
  question: EditorQuestion
  readOnly: boolean
  onChange: (patch: Partial<EditorQuestion>) => void
}) {
  return (
    <div className="mt-4 rounded-lg border border-surface-border bg-white p-3">
      <p className={LABEL}>Choices</p>
      <div className="space-y-2">
        {question.options.map((option, optionIndex) => (
          <div key={option.id} className="flex items-center gap-2">
            <span className="w-5 text-xs text-ink-muted">{optionIndex + 1}.</span>
            <input
              value={option.label}
              disabled={readOnly}
              placeholder="Answer people can pick"
              onChange={(event) =>
                onChange({
                  options: question.options.map((o, i) =>
                    i === optionIndex ? { ...o, label: event.target.value } : o,
                  ),
                })
              }
              className={FIELD}
            />
            {!readOnly && (
              <button
                type="button"
                onClick={() =>
                  onChange({ options: question.options.filter((_, i) => i !== optionIndex) })
                }
                className="shrink-0 rounded px-2 py-2 text-xs text-ink-muted hover:text-red-600"
              >
                Remove
              </button>
            )}
          </div>
        ))}
      </div>
      {!readOnly && (
        <Button
          variant="ghost"
          className="mt-2"
          onClick={() => onChange({ options: [...question.options, newOption()] })}
        >
          Add choice
        </Button>
      )}
    </div>
  )
}

const OPERATOR_LABELS: Record<'eq' | 'ne' | 'in', string> = {
  eq: 'is',
  ne: 'is not',
  in: 'is any of',
}

/**
 * Replaces the raw display-rule JSON. Only the shape DisplayRuleEvaluator understands can be
 * built here - an AND of is / is not / is any of - which is also the only shape the server accepts.
 */
function ConditionsEditor({
  question,
  others,
  readOnly,
  onChange,
}: {
  question: EditorQuestion
  others: CatalogEntry[]
  readOnly: boolean
  onChange: (patch: Partial<EditorQuestion>) => void
}) {
  if (question.unsupportedDisplayRule) {
    return (
      <p className="mt-4 rounded-lg bg-amber-50 px-3 py-2 text-xs text-amber-800">
        This question has a custom show/hide rule that this editor can’t display. It will be kept
        exactly as it is when you save.
      </p>
    )
  }

  const updateCondition = (index: number, patch: Partial<(typeof question.conditions)[number]>) =>
    onChange({
      conditions: question.conditions.map((condition, i) =>
        i === index ? { ...condition, ...patch } : condition,
      ),
    })

  return (
    <div className="mt-4">
      <p className={LABEL}>Always show this question?</p>
      {question.conditions.length === 0 ? (
        <p className="text-xs text-ink-muted">
          Yes — everyone sees it.{' '}
          {!readOnly && others.length > 0 && (
            <button
              type="button"
              onClick={() => onChange({ conditions: [newCondition()] })}
              className="font-medium text-magenta-600 hover:underline"
            >
              Only show it sometimes
            </button>
          )}
          {!readOnly && others.length === 0 && 'Add another question first to make this conditional.'}
        </p>
      ) : (
        <div className="space-y-2 rounded-lg border border-surface-border bg-white p-3">
          <p className="text-xs text-ink-muted">Only show this question when all of these are true:</p>
          {question.conditions.map((condition, conditionIndex) => {
            const referenced = others.find((entry) => entry.id === condition.questionId)
            return (
              <div key={condition.id} className="flex flex-wrap items-center gap-2">
                <select
                  value={condition.questionId}
                  disabled={readOnly}
                  onChange={(event) =>
                    updateCondition(conditionIndex, { questionId: event.target.value, values: [''] })
                  }
                  className={`${FIELD} w-56`}
                >
                  <option value="">Choose a question…</option>
                  {others.map((entry) => (
                    <option key={entry.id} value={entry.id}>
                      {entry.prompt}
                    </option>
                  ))}
                </select>

                <select
                  value={condition.op}
                  disabled={readOnly}
                  onChange={(event) =>
                    updateCondition(conditionIndex, {
                      op: event.target.value as 'eq' | 'ne' | 'in',
                      values: [condition.values[0] ?? ''],
                    })
                  }
                  className={`${FIELD} w-28`}
                >
                  {(['eq', 'ne', 'in'] as const).map((op) => (
                    <option key={op} value={op}>
                      {OPERATOR_LABELS[op]}
                    </option>
                  ))}
                </select>

                <ConditionValue
                  referenced={referenced}
                  values={condition.values}
                  multiple={condition.op === 'in'}
                  readOnly={readOnly}
                  onChange={(values) => updateCondition(conditionIndex, { values })}
                />

                {!readOnly && (
                  <button
                    type="button"
                    onClick={() =>
                      onChange({
                        conditions: question.conditions.filter((_, i) => i !== conditionIndex),
                      })
                    }
                    className="rounded px-2 py-2 text-xs text-ink-muted hover:text-red-600"
                  >
                    Remove
                  </button>
                )}
              </div>
            )
          })}
          {!readOnly && (
            <Button
              variant="ghost"
              onClick={() => onChange({ conditions: [...question.conditions, newCondition()] })}
            >
              Add another condition
            </Button>
          )}
        </div>
      )}
    </div>
  )
}

/** Offers the referenced question's own answers where it has a fixed set, free text otherwise. */
function ConditionValue({
  referenced,
  values,
  multiple,
  readOnly,
  onChange,
}: {
  referenced: CatalogEntry | undefined
  values: string[]
  multiple: boolean
  readOnly: boolean
  onChange: (values: string[]) => void
}) {
  if (!referenced) {
    return <span className="text-xs text-ink-muted">Pick a question first</span>
  }

  const fixedChoices: { value: string; label: string }[] | null = isChoiceType(referenced.type)
    ? referenced.options.map((option) => ({ value: option.code, label: option.label }))
    : referenced.type === 'BOOLEAN'
      ? [
          { value: 'true', label: 'Yes' },
          { value: 'false', label: 'No' },
        ]
      : null

  if (!fixedChoices) {
    return (
      <input
        value={values.join(', ')}
        disabled={readOnly}
        placeholder={multiple ? 'value, value' : 'value'}
        onChange={(event) =>
          onChange(multiple ? event.target.value.split(',').map((v) => v.trim()) : [event.target.value])
        }
        className={`${FIELD} w-48`}
      />
    )
  }

  if (multiple) {
    return (
      <div className="flex flex-wrap gap-2 rounded-lg border border-surface-border px-2 py-1.5">
        {fixedChoices.map((choice) => (
          <label key={choice.value} className="flex items-center gap-1 text-xs text-ink">
            <input
              type="checkbox"
              disabled={readOnly}
              checked={values.includes(choice.value)}
              onChange={(event) =>
                onChange(
                  event.target.checked
                    ? [...values.filter((v) => v !== ''), choice.value]
                    : values.filter((v) => v !== choice.value),
                )
              }
              className="h-3.5 w-3.5 rounded border-surface-border"
            />
            {choice.label}
          </label>
        ))}
      </div>
    )
  }

  return (
    <select
      value={values[0] ?? ''}
      disabled={readOnly}
      onChange={(event) => onChange([event.target.value])}
      className={`${FIELD} w-48`}
    >
      <option value="">Choose…</option>
      {fixedChoices.map((choice) => (
        <option key={choice.value} value={choice.value}>
          {choice.label}
        </option>
      ))}
    </select>
  )
}

/**
 * Everything an author does not need: the identifier, and the scoring fields that only matter
 * once the insight engine exists.
 */
function AdvancedPanel({
  question,
  code,
  codes,
}: {
  question: EditorQuestion
  code: string
  codes: AssignedCodes
}) {
  const [open, setOpen] = useState(false)

  return (
    <div className="mt-4 border-t border-surface-border pt-3">
      <button
        type="button"
        onClick={() => setOpen((current) => !current)}
        className="text-xs font-medium text-ink-muted hover:text-ink"
      >
        {open ? '▾' : '▸'} Advanced
      </button>

      {open && (
        <div className="mt-3 space-y-4">
          <div>
            <p className={LABEL}>Identifier</p>
            <code className="rounded bg-white px-2 py-1 text-xs text-ink">{code || '—'}</code>
            <span className={HINT}>
              {question.codeLocked
                ? 'Fixed. Answers already given are matched to this question by its identifier, so it stays the same however you reword the question.'
                : 'Generated from your wording. It becomes permanent once this version is published, so that answers stay matched to this question in future versions.'}
            </span>
          </div>

          {isChoiceType(question.type) && question.options.length > 0 && (
            <div>
              <p className={LABEL}>Scoring (optional)</p>
              <span className={HINT}>
                Used by the scoring engine, which isn’t built yet. Safe to leave blank.
              </span>
              <table className="mt-2 w-full text-xs">
                <thead>
                  <tr className="text-left text-ink-muted">
                    <th className="py-1 font-medium">Choice</th>
                    <th className="py-1 font-medium">Score</th>
                    <th className="py-1 font-medium">Signals</th>
                  </tr>
                </thead>
                <tbody>
                  {question.options.map((option) => (
                    <tr key={option.id}>
                      <td className="py-1 pr-2 text-ink">{option.label || '—'}</td>
                      <td className="py-1 pr-2 text-ink-muted">{option.score || '—'}</td>
                      <td className="py-1 text-ink-muted">{option.signals || '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <span className={HINT}>
                Identifiers: {question.options.map((o) => codes.options.get(o.id)).join(', ')}
              </span>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

function ReorderControls({
  index,
  total,
  onMove,
  onRemove,
  removeLabel,
}: {
  index: number
  total: number
  onMove: (delta: number) => void
  onRemove: () => void
  removeLabel: string
}) {
  return (
    <div className="flex gap-1 text-xs">
      <button
        type="button"
        onClick={() => onMove(-1)}
        disabled={index === 0}
        aria-label="Move up"
        className="rounded px-2 py-1 text-ink-muted hover:bg-white disabled:opacity-40"
      >
        ↑
      </button>
      <button
        type="button"
        onClick={() => onMove(1)}
        disabled={index === total - 1}
        aria-label="Move down"
        className="rounded px-2 py-1 text-ink-muted hover:bg-white disabled:opacity-40"
      >
        ↓
      </button>
      <button
        type="button"
        onClick={onRemove}
        className="rounded px-2 py-1 text-ink-muted hover:bg-white hover:text-red-600"
      >
        {removeLabel}
      </button>
    </div>
  )
}
