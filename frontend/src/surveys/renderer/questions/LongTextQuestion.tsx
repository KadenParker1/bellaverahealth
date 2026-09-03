import { QuestionShell, type QuestionProps } from './QuestionShell'

export function LongTextQuestion({ question, value, onChange, error }: QuestionProps) {
  return (
    <QuestionShell question={question} error={error}>
      <textarea
        rows={4}
        maxLength={question.config.maxLength as number | undefined}
        value={typeof value === 'string' ? value : ''}
        onChange={(e) => onChange(e.target.value)}
        className="w-full rounded-lg border border-surface-border bg-white px-3 py-2.5 text-ink outline-none focus:border-magenta-500 focus:ring-1 focus:ring-magenta-500"
      />
    </QuestionShell>
  )
}
