import { QUESTION_COMPONENTS } from '../surveys/renderer/questionRegistry'
import type { QuestionType } from '../types/api'

/**
 * Plain-English names for the question types. The registry still decides which types exist - this
 * only decides what an author is shown instead of SINGLE_CHOICE.
 */
export const QUESTION_TYPE_LABELS: Record<QuestionType, string> = {
  SINGLE_CHOICE: 'Pick one',
  MULTI_CHOICE: 'Pick any that apply',
  SCALE: 'Rating scale',
  NUMBER: 'Number',
  TEXT: 'Short text',
  LONG_TEXT: 'Long text',
  DATE: 'Date',
  BOOLEAN: 'Yes / No',
}

export const QUESTION_TYPES = Object.keys(QUESTION_COMPONENTS) as QuestionType[]

export function isChoiceType(type: QuestionType): boolean {
  return type === 'SINGLE_CHOICE' || type === 'MULTI_CHOICE'
}
