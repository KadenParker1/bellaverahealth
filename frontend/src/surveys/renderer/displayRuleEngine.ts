import type { DisplayRule } from '../../types/api'
import type { AnswersState } from '../types'

// Mirrors backend com.pm.bellavera.response.DisplayRuleEvaluator: only "all" (AND) of
// eq/ne/in comparisons is supported; an unknown op or unmet shape defaults to visible.
export function isVisible(displayRule: DisplayRule | null, answers: AnswersState): boolean {
  if (!displayRule || !displayRule.all || displayRule.all.length === 0) return true

  return displayRule.all.every((condition) => {
    const actual = answers[condition.questionCode]
    switch (condition.op) {
      case 'eq':
        return String(actual) === String(condition.value)
      case 'ne':
        return String(actual) !== String(condition.value)
      case 'in':
        return Array.isArray(condition.value) && condition.value.some((v) => String(v) === String(actual))
      default:
        return true
    }
  })
}
