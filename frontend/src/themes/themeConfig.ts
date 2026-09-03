import type { SurveyTheme } from '../types/api'
import exerciseImg from '../assets/themes/exercise.jpg'
import nutritionImg from '../assets/themes/nutrition.jpg'
import hormonesImg from '../assets/themes/hormones.jpg'
import pelvicFloorImg from '../assets/themes/pelvic-floor.jpg'

export interface ThemeConfigEntry {
  slug: string
  theme: SurveyTheme
  label: string
  image: string
}

export const THEME_CONFIG: ThemeConfigEntry[] = [
  { slug: 'exercise', theme: 'EXERCISE', label: 'Exercise', image: exerciseImg },
  { slug: 'nutrition', theme: 'NUTRITION', label: 'Nutrition', image: nutritionImg },
  { slug: 'hormones', theme: 'HORMONES', label: 'Hormones', image: hormonesImg },
  { slug: 'pelvic-floor', theme: 'PELVIC_FLOOR', label: 'Pelvic Floor', image: pelvicFloorImg },
]

export function themeBySlug(slug: string | undefined): ThemeConfigEntry | undefined {
  return THEME_CONFIG.find((t) => t.slug === slug)
}
