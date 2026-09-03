import { QuestionShell, type QuestionProps } from './QuestionShell'

export function SingleChoiceQuestion({ question, value, onChange, error }: QuestionProps) {
  return (
    <QuestionShell question={question} error={error}>
      <div className="space-y-2">
        {question.options.map((option) => (
          <label
            key={option.code}
            className="flex cursor-pointer items-center gap-2 rounded-lg border border-surface-border px-3 py-2 has-[:checked]:border-magenta-500 has-[:checked]:bg-magenta-100"
          >
            <input
              type="radio"
              name={question.code}
              value={option.code}
              checked={value === option.code}
              onChange={() => onChange(option.code)}
              className="accent-magenta-500"
            />
            <span className="text-sm text-ink">{option.label}</span>
          </label>
        ))}
      </div>
    </QuestionShell>
  )
}
