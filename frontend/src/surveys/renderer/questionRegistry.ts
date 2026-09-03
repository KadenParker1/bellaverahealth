import type { ComponentType } from 'react'
import type { QuestionType } from '../../types/api'
import type { QuestionProps } from './questions/QuestionShell'
import { SingleChoiceQuestion } from './questions/SingleChoiceQuestion'
import { MultiChoiceQuestion } from './questions/MultiChoiceQuestion'
import { ScaleQuestion } from './questions/ScaleQuestion'
import { NumberQuestion } from './questions/NumberQuestion'
import { TextQuestion } from './questions/TextQuestion'
import { LongTextQuestion } from './questions/LongTextQuestion'
import { DateQuestion } from './questions/DateQuestion'
import { BooleanQuestion } from './questions/BooleanQuestion'

export const QUESTION_COMPONENTS: Record<QuestionType, ComponentType<QuestionProps>> = {
  SINGLE_CHOICE: SingleChoiceQuestion,
  MULTI_CHOICE: MultiChoiceQuestion,
  SCALE: ScaleQuestion,
  NUMBER: NumberQuestion,
  TEXT: TextQuestion,
  LONG_TEXT: LongTextQuestion,
  DATE: DateQuestion,
  BOOLEAN: BooleanQuestion,
}
