import type { AdminSectionDto, DisplayRule, SectionDto, SurveyDetailDto } from '../types/api'

/**
 * Adapts the editor's own draft state - already run through {@link toSectionDtos}, the same
 * function `Save draft` sends to the server - into the shape {@link SurveyRenderer} expects, so a
 * non-technical author can see a draft exactly as a user would before ever publishing it.
 *
 * `surveyId`/`versionId`/`title`/`theme`/`code` below are never read by `SurveyRenderer` itself -
 * it only renders `sections` - so placeholder values are fine here.
 */
export function toPreviewSurveyDetail(surveyId: string, versionId: string, sections: AdminSectionDto[]): SurveyDetailDto {
  return {
    surveyId,
    versionId,
    version: 0,
    code: 'preview',
    theme: 'EXERCISE',
    title: 'Preview',
    description: '',
    sections: sections.map(toPreviewSection),
  }
}

function toPreviewSection(section: AdminSectionDto): SectionDto {
  return {
    code: section.code,
    title: section.title,
    description: section.description,
    questions: section.questions.map((question) => ({
      code: question.code,
      type: question.type,
      prompt: question.prompt,
      helpText: question.helpText,
      required: question.required,
      sortOrder: question.sortOrder,
      config: question.config ?? {},
      displayRule: (question.displayRule as DisplayRule | null) ?? null,
      options: question.options.map((option) => ({
        code: option.code,
        label: option.label,
        sortOrder: option.sortOrder,
      })),
    })),
  }
}
