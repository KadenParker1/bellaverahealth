import { QuestionShell, type QuestionProps } from './QuestionShell'

export function BooleanQuestion({ question, value, onChange, error }: QuestionProps) {
  return (
    <QuestionShell question={question} error={error}>
      <div className="flex gap-2">
        {[
          { label: 'Yes', v: true },
          { label: 'No', v: false },
        ].map(({ label, v }) => (
          <button
            key={label}
            type="button"
            onClick={() => onChange(v)}
            className={`rounded-lg border px-4 py-2 text-sm font-medium transition-colors ${
              value === v
                ? 'border-magenta-500 bg-magenta-500 text-white'
                : 'border-surface-border bg-white text-ink hover:border-magenta-500'
            }`}
          >
            {label}
          </button>
        ))}
      </div>
    </QuestionShell>
  )
}
