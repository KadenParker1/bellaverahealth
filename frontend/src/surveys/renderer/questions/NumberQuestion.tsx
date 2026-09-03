import { QuestionShell, type QuestionProps } from './QuestionShell'

export function NumberQuestion({ question, value, onChange, error }: QuestionProps) {
  const unit = question.config.unit as string | undefined
  return (
    <QuestionShell question={question} error={error}>
      <div className="flex items-center gap-2">
        <input
          type="number"
          min={question.config.min as number | undefined}
          max={question.config.max as number | undefined}
          value={typeof value === 'number' ? value : ''}
          onChange={(e) => onChange(e.target.value === '' ? undefined : Number(e.target.value))}
          className="w-32 rounded-lg border border-surface-border bg-white px-3 py-2.5 text-ink outline-none focus:border-magenta-500 focus:ring-1 focus:ring-magenta-500"
        />
        {unit && <span className="text-sm text-ink-muted">{unit}</span>}
      </div>
    </QuestionShell>
  )
}
