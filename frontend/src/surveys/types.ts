import type { AnswerDto, AnswerRequest, QuestionDto } from '../types/api'

export type AnswerValue = string | string[] | number | boolean | undefined

export type AnswersState = Record<string, AnswerValue>

export function initialAnswersFrom(answers: AnswerDto[] | undefined): AnswersState {
  const state: AnswersState = {}
  if (!answers) return state
  for (const answer of answers) {
    state[answer.questionCode] = valueFromAnswer(answer)
  }
  return state
}

function valueFromAnswer(answer: AnswerDto): AnswerValue {
  if (answer.optionCodes && answer.optionCodes.length > 0) {
    return answer.optionCodes.length === 1 ? answer.optionCodes[0] : answer.optionCodes
  }
  if (answer.valueBoolean !== undefined && answer.valueBoolean !== null) return answer.valueBoolean
  if (answer.valueNumber !== undefined && answer.valueNumber !== null) return answer.valueNumber
  if (answer.valueDate) return answer.valueDate
  if (answer.valueText !== undefined && answer.valueText !== null) return answer.valueText
  return undefined
}

export function toAnswerRequest(question: QuestionDto, value: AnswerValue): AnswerRequest {
  const base: AnswerRequest = { questionCode: question.code }
  switch (question.type) {
    case 'SINGLE_CHOICE':
      return { ...base, optionCodes: value ? [value as string] : [] }
    case 'MULTI_CHOICE':
      return { ...base, optionCodes: (value as string[] | undefined) ?? [] }
    case 'SCALE':
    case 'NUMBER':
      return { ...base, valueNumber: typeof value === 'number' ? value : undefined }
    case 'DATE':
      return { ...base, valueDate: (value as string | undefined) || undefined }
    case 'BOOLEAN':
      return { ...base, valueBoolean: typeof value === 'boolean' ? value : undefined }
    case 'TEXT':
    case 'LONG_TEXT':
    default:
      return { ...base, valueText: (value as string | undefined) || undefined }
  }
}

export function isAnswered(value: AnswerValue): boolean {
  if (value === undefined || value === null) return false
  if (typeof value === 'string') return value.trim().length > 0
  if (Array.isArray(value)) return value.length > 0
  return true
}
