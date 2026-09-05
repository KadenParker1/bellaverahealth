// Hand-maintained mirror of the backend DTOs.
//
// The API sets `spring.jackson.default-property-inclusion=non_null`, so a null field is *absent*
// from the JSON, not present as null. A `| null` below therefore means "may be missing" too:
// normalize with `??` before comparing (`x ?? null`), because `x === null` is false for an
// absent field and `x.foo` on it throws.

export type UnitSystem = 'METRIC' | 'IMPERIAL'

export type UserRole = 'USER' | 'ADMIN'

/** SUSPENDED is a ban: the API refuses every request from the account until it is ACTIVE again. */
export type UserStatus = 'ACTIVE' | 'SUSPENDED' | 'DELETED'

export interface UserProfileResponse {
  userId: string
  email: string
  role: UserRole
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

// ---------------------------------------------------------------- store

export interface ProductDto {
  id: string
  code: string
  name: string
  description: string | null
  imageUrl: string | null
  /** Minor units. Money never crosses the wire as a decimal. */
  priceCents: number
  currency: string
  /** Units still sellable (stock minus what paid-but-unshipped orders owe). Null = untracked. */
  available: number | null
}

export interface CheckoutItemRequest {
  productCode: string
  quantity: number
}

export interface CheckoutRequest {
  items: CheckoutItemRequest[]
}

export interface CheckoutSessionDto {
  orderId: string
  checkoutUrl: string
}

export type OrderStatus = 'PENDING' | 'PAID' | 'FULFILLED' | 'CANCELLED'

export interface OrderItemDto {
  productCode: string
  productName: string
  unitPriceCents: number
  quantity: number
  lineTotalCents: number
}

export interface ShippingAddressDto {
  name: string | null
  line1: string | null
  line2: string | null
  city: string | null
  region: string | null
  postalCode: string | null
  country: string | null
}

export interface OrderDto {
  id: string
  status: OrderStatus
  currency: string
  subtotalCents: number
  placedAt: string
  paidAt: string | null
  fulfilledAt: string | null
  carrier: string | null
  trackingNumber: string | null
  shipTo: ShippingAddressDto | null
  items: OrderItemDto[]
}

export interface AdminOrderDto extends OrderDto {
  customerUserId: string
  customerEmail: string | null
}

export interface FulfillOrderRequest {
  carrier?: string
  trackingNumber?: string
}

// ---------------------------------------------------------------- admin

export interface AdminProductDto {
  id: string
  code: string
  name: string
  description: string | null
  imageUrl: string | null
  priceCents: number
  currency: string
  stripePriceId: string | null
  active: boolean
  sortOrder: number
  /** Units on hand. Null when this product is not stock-tracked. */
  stockQuantity: number | null
  /** Stock minus what paid-but-unshipped orders already owe. Null when untracked. */
  available: number | null
}

export interface CreateProductRequest {
  code: string
  name: string
  description?: string
  imageUrl?: string
  priceCents: number
  currency?: string
  stripePriceId?: string
  sortOrder?: number
  /** Omit for a product that is not stock-tracked. */
  stockQuantity?: number | null
}

export interface UpdateProductRequest {
  name?: string
  description?: string
  imageUrl?: string
  priceCents?: number
  currency?: string
  stripePriceId?: string
  active?: boolean
  sortOrder?: number
  stockQuantity?: number
  /** Stops tracking stock. Needed because null stockQuantity already means "leave unchanged". */
  clearStock?: boolean
}

export type SurveyVersionStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'

export interface AdminVersionSummaryDto {
  versionId: string
  version: number
  status: SurveyVersionStatus
  publishedAt: string | null
  questionCount: number
}

export interface AdminSurveyDto {
  surveyId: string
  code: string
  theme: SurveyTheme
  title: string
  description: string | null
  sortOrder: number
  active: boolean
  versions: AdminVersionSummaryDto[]
}

export interface AdminOptionDto {
  code: string
  label: string
  sortOrder: number
  valueNumeric: number | null
  metadata: Record<string, unknown> | null
}

export interface AdminQuestionDto {
  code: string
  type: QuestionType
  prompt: string
  helpText: string | null
  required: boolean
  sortOrder: number
  config: Record<string, unknown> | null
  displayRule: Record<string, unknown> | null
  options: AdminOptionDto[]
}

export interface AdminSectionDto {
  code: string
  title: string
  description: string | null
  sortOrder: number
  questions: AdminQuestionDto[]
}

export interface AdminSurveyVersionDto {
  surveyId: string
  versionId: string
  version: number
  status: SurveyVersionStatus
  publishedAt: string | null
  notes: string | null
  sections: AdminSectionDto[]
}

export interface CreateSurveyRequest {
  code: string
  theme: SurveyTheme
  title: string
  description?: string
  sortOrder?: number
}

export interface UpdateSurveyRequest {
  title?: string
  description?: string
  sortOrder?: number
  active?: boolean
}

export interface AdminUserDto {
  userId: string
  email: string
  displayName: string | null
  role: UserRole
  status: UserStatus
  createdAt: string
  onboardingCompletedAt: string | null
}

export interface UpdateUserStatusRequest {
  status: UserStatus
  reason?: string
}

export interface SaveVersionContentRequest {
  notes?: string
  sections: AdminSectionDto[]
}

export interface ProblemDetail {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
  errors?: string[]
  /** Set on 5xx and other opaque failures; quote it to find the incident in the server log. */
  errorId?: string
  /** Set on 429. */
  retryAfterSeconds?: number
}
