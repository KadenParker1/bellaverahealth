import type { ReactNode } from 'react'
import type { QuestionDto } from '../../../types/api'
import type { AnswerValue } from '../../types'

export interface QuestionProps {
  question: QuestionDto
  value: AnswerValue
  onChange: (value: AnswerValue) => void
  error?: string
}

export function QuestionShell({
  question,
  error,
  children,
}: {
  question: QuestionDto
  error?: string
  children: ReactNode
}) {
  return (
    <div>
      <label className="mb-1.5 block text-sm font-medium text-ink">
        {question.prompt}
        {question.required && <span className="text-magenta-600"> *</span>}
      </label>
      {question.helpText && <p className="mb-2 text-sm text-ink-muted">{question.helpText}</p>}
      {children}
      {error && <p className="mt-1.5 text-sm text-red-600">{error}</p>}
    </div>
  )
}
