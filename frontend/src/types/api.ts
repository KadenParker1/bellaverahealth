export type UnitSystem = 'METRIC' | 'IMPERIAL'

export interface UserProfileResponse {
  userId: string
  email: string
  displayName: string | null
  birthYear: number | null
  country: string | null
  timezone: string | null
  unitSystem: UnitSystem
  onboardingCompletedAt: string | null
  consentTermsAt: string | null
  consentAiAt: string | null
}

export interface UpdateProfileRequest {
  displayName?: string
  birthYear?: number
  country?: string
  timezone?: string
  unitSystem?: UnitSystem
  acceptTerms?: boolean
  acceptAiConsent?: boolean
}

export type SurveyTheme =
  | 'ONBOARDING'
  | 'EXERCISE'
  | 'NUTRITION'
  | 'HORMONES'
  | 'PELVIC_FLOOR'

export interface SurveySummaryDto {
  surveyId: string
  code: string
  theme: SurveyTheme
  title: string
  description: string
  publishedVersionId: string
  completed: boolean
}

export type QuestionType =
  | 'SINGLE_CHOICE'
  | 'MULTI_CHOICE'
  | 'SCALE'
  | 'NUMBER'
  | 'TEXT'
  | 'LONG_TEXT'
  | 'DATE'
  | 'BOOLEAN'

export interface QuestionOptionDto {
  code: string
  label: string
  sortOrder: number
}

export interface DisplayRuleCondition {
  questionCode: string
  op: 'eq' | 'ne' | 'in' | string
  value: unknown
}

export interface DisplayRule {
  all: DisplayRuleCondition[]
}

export interface QuestionDto {
  code: string
  type: QuestionType
  prompt: string
  helpText: string | null
  required: boolean
  sortOrder: number
  config: Record<string, unknown>
  displayRule: DisplayRule | null
  options: QuestionOptionDto[]
}

export interface SectionDto {
  code: string
  title: string
  description: string | null
  questions: QuestionDto[]
}

export interface SurveyDetailDto {
  surveyId: string
  versionId: string
  version: number
  code: string
  theme: SurveyTheme
  title: string
  description: string
  sections: SectionDto[]
}

export type ResponseStatus = 'IN_PROGRESS' | 'SUBMITTED'

export interface AnswerRequest {
  questionCode: string
  valueText?: string
  valueNumber?: number
  valueBoolean?: boolean
  valueDate?: string
  optionCodes?: string[]
}

export type AnswerDto = AnswerRequest

export interface SubmitResponseRequest {
  status: ResponseStatus
  answers: AnswerRequest[]
}

export interface SurveyResponseDetailDto {
  responseId: string
  status: ResponseStatus
  startedAt: string
  submittedAt: string | null
  answers: AnswerDto[]
}

export type InsightBand = 'LOW' | 'MODERATE' | 'HIGH' | 'UNKNOWN'

export interface InsightDto {
  domain: string
  code: string
  label: string
  score: number | null
  band: InsightBand
  confidence: number | null
  rationale: string | null
}

export interface ChatRequest {
  threadId?: string
  message: string
}

export interface ChatResponseDto {
  threadId: string
  reply: string
  messageId: string
}

export interface ChatThreadSummaryDto {
  id: string
  title: string | null
  createdAt: string
  lastMessageAt: string | null
}

export type ChatRole = 'USER' | 'ASSISTANT' | 'SYSTEM'

export interface ChatMessageDto {
  id: string
  role: ChatRole
  content: string
  createdAt: string
}

export interface ProblemDetail {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
  errors?: string[]
}
