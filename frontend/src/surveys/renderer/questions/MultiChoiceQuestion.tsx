import { QuestionShell, type QuestionProps } from './QuestionShell'

export function MultiChoiceQuestion({ question, value, onChange, error }: QuestionProps) {
  const selected = Array.isArray(value) ? value : []

  function toggle(code: string) {
    if (selected.includes(code)) {
      onChange(selected.filter((c) => c !== code))
    } else {
      onChange([...selected, code])
    }
  }

  return (
    <QuestionShell question={question} error={error}>
      <div className="space-y-2">
        {question.options.map((option) => (
          <label
            key={option.code}
            className="flex cursor-pointer items-center gap-2 rounded-lg border border-surface-border px-3 py-2 has-[:checked]:border-magenta-500 has-[:checked]:bg-magenta-100"
          >
            <input
              type="checkbox"
              checked={selected.includes(option.code)}
              onChange={() => toggle(option.code)}
              className="accent-magenta-500"
            />
            <span className="text-sm text-ink">{option.label}</span>
          </label>
        ))}
      </div>
    </QuestionShell>
  )
}
