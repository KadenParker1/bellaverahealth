import { QuestionShell, type QuestionProps } from './QuestionShell'

export function ScaleQuestion({ question, value, onChange, error }: QuestionProps) {
  const min = Number(question.config.min ?? 1)
  const max = Number(question.config.max ?? 5)
  const scaleLabels = (question.config.scaleLabels as Record<string, string> | undefined) ?? {}
  const options = Array.from({ length: max - min + 1 }, (_, i) => min + i)

  return (
    <QuestionShell question={question} error={error}>
      <div className="flex flex-wrap gap-2">
        {options.map((n) => (
          <button
            key={n}
            type="button"
            onClick={() => onChange(n)}
            title={scaleLabels[String(n)]}
            className={`flex h-11 w-11 items-center justify-center rounded-lg border text-sm font-medium transition-colors ${
              value === n
                ? 'border-magenta-500 bg-magenta-500 text-white'
                : 'border-surface-border bg-white text-ink hover:border-magenta-500'
            }`}
          >
            {n}
          </button>
        ))}
      </div>
      {(scaleLabels[String(min)] || scaleLabels[String(max)]) && (
        <div className="mt-1.5 flex justify-between text-xs text-ink-muted">
          <span>{scaleLabels[String(min)]}</span>
          <span>{scaleLabels[String(max)]}</span>
        </div>
      )}
    </QuestionShell>
  )
}
