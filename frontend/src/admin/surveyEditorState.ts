import type {
  AdminOptionDto,
  AdminQuestionDto,
  AdminSectionDto,
  AdminSurveyVersionDto,
  QuestionType,
} from '../types/api'
import { isChoiceType } from './questionTypes'

/**
 * The editor's own shape.
 *
 * Nothing here is JSON an author has to type. The `config` JSONB is split into the handful of
 * keys the renderer actually reads (min/max/unit/maxLength/scale end labels), a display rule is
 * held as a list of conditions, and option `metadata.signals` is a comma-separated string. Keys
 * we do not model are carried in `configExtra` / `metadataExtra` and written back untouched, so
 * editing a survey never silently drops something the editor did not understand.
 */

export interface EditorCondition {
  id: string
  /**
   * The editor id of the question this depends on, not its code. An unlocked question's code is
   * derived from its wording, so storing the code here would break the rule the moment somebody
   * reworded the question it points at. Resolved to a code at save time.
   */
  questionId: string
  op: 'eq' | 'ne' | 'in'
  values: string[]
}

export interface EditorOption {
  id: string
  code: string
  /** True once the option exists server-side: its code is a join key and must not drift. */
  codeLocked: boolean
  label: string
  score: string
  signals: string
  metadataExtra: Record<string, unknown>
}

export interface EditorQuestion {
  id: string
  code: string
  codeLocked: boolean
  type: QuestionType
  prompt: string
  helpText: string
  required: boolean
  min: string
  max: string
  unit: string
  maxLength: string
  scaleMinLabel: string
  scaleMaxLabel: string
  configExtra: Record<string, unknown>
  conditions: EditorCondition[]
  /** A display rule in a shape this builder cannot represent, preserved verbatim. */
  unsupportedDisplayRule: Record<string, unknown> | null
  options: EditorOption[]
}

export interface EditorSection {
  id: string
  code: string
  codeLocked: boolean
  title: string
  description: string
  questions: EditorQuestion[]
}

let idCounter = 0
/** React list keys only - never sent to the server, which keys off `code`. */
function nextId(prefix: string): string {
  idCounter += 1
  return `${prefix}-${idCounter}`
}

// --- identifiers ---------------------------------------------------------

/**
 * Turns wording into an identifier. Authors never type these: a new item derives its code from
 * its own text, and an existing item keeps the code it was published with.
 */
export function slugify(text: string): string {
  return text
    .toLowerCase()
    .replace(/['']/g, '')
    .replace(/[^a-z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '')
    .slice(0, 48)
}

function uniqueCode(candidate: string, fallback: string, used: Set<string>): string {
  const base = candidate || fallback
  let code = base
  let suffix = 2
  while (used.has(code)) {
    code = `${base}_${suffix}`
    suffix += 1
  }
  used.add(code)
  return code
}

export interface AssignedCodes {
  sections: Map<string, string>
  questions: Map<string, string>
  options: Map<string, string>
}

/**
 * Works out the code every item will be saved with. Locked codes win as-is; unlocked ones are
 * derived from their wording, then de-duplicated. The editor shows the result so an author can
 * see the identifier without ever editing it.
 */
export function assignCodes(sections: EditorSection[]): AssignedCodes {
  const assigned: AssignedCodes = { sections: new Map(), questions: new Map(), options: new Map() }
  const usedSections = new Set<string>()
  const usedQuestions = new Set<string>()

  sections.forEach((section, sectionIndex) => {
    const code = section.codeLocked
      ? uniqueCode(section.code, `section_${sectionIndex + 1}`, usedSections)
      : uniqueCode(slugify(section.title), `section_${sectionIndex + 1}`, usedSections)
    assigned.sections.set(section.id, code)

    section.questions.forEach((question, questionIndex) => {
      const questionCode = question.codeLocked
        ? uniqueCode(question.code, `question_${questionIndex + 1}`, usedQuestions)
        : uniqueCode(slugify(question.prompt), `question_${questionIndex + 1}`, usedQuestions)
      assigned.questions.set(question.id, questionCode)

      const usedOptions = new Set<string>()
      question.options.forEach((option, optionIndex) => {
        const optionCode = option.codeLocked
          ? uniqueCode(option.code, `option_${optionIndex + 1}`, usedOptions)
          : uniqueCode(slugify(option.label), `option_${optionIndex + 1}`, usedOptions)
        assigned.options.set(option.id, optionCode)
      })
    })
  })

  return assigned
}

// --- loading -------------------------------------------------------------

const KNOWN_CONFIG_KEYS = ['min', 'max', 'unit', 'maxLength', 'scaleLabels']

function numberToText(value: unknown): string {
  return typeof value === 'number' ? String(value) : typeof value === 'string' ? value : ''
}

function toEditorOption(option: AdminOptionDto): EditorOption {
  const metadata = { ...(option.metadata ?? {}) }
  const signals = Array.isArray(metadata.signals) ? (metadata.signals as unknown[]) : []
  delete metadata.signals

  return {
    id: nextId('option'),
    code: option.code,
    codeLocked: true,
    label: option.label,
    score: option.valueNumeric === null || option.valueNumeric === undefined ? '' : String(option.valueNumeric),
    signals: signals.map(String).join(', '),
    metadataExtra: metadata,
  }
}

function toEditorQuestion(question: AdminQuestionDto): EditorQuestion {
  const config = { ...(question.config ?? {}) }
  const scaleLabels = (config.scaleLabels ?? {}) as Record<string, unknown>
  const min = numberToText(config.min)
  const max = numberToText(config.max)

  const configExtra: Record<string, unknown> = {}
  for (const [key, value] of Object.entries(config)) {
    if (!KNOWN_CONFIG_KEYS.includes(key)) configExtra[key] = value
  }

  const { conditions, unsupported } = toConditions(question.displayRule ?? null)

  return {
    id: nextId('question'),
    code: question.code,
    codeLocked: true,
    type: question.type,
    prompt: question.prompt,
    helpText: question.helpText ?? '',
    required: question.required,
    min,
    max,
    unit: typeof config.unit === 'string' ? config.unit : '',
    maxLength: numberToText(config.maxLength),
    scaleMinLabel: min && scaleLabels[min] !== undefined ? String(scaleLabels[min]) : '',
    scaleMaxLabel: max && scaleLabels[max] !== undefined ? String(scaleLabels[max]) : '',
    configExtra,
    conditions,
    unsupportedDisplayRule: unsupported,
    options: question.options.map(toEditorOption),
  }
}

function toConditions(rule: Record<string, unknown> | null): {
  conditions: EditorCondition[]
  unsupported: Record<string, unknown> | null
} {
  if (!rule || Object.keys(rule).length === 0) return { conditions: [], unsupported: null }
  const all = rule.all
  if (!Array.isArray(all)) return { conditions: [], unsupported: rule }

  const conditions: EditorCondition[] = []
  for (const raw of all) {
    if (typeof raw !== 'object' || raw === null) return { conditions: [], unsupported: rule }
    const { questionCode, op, value } = raw as Record<string, unknown>
    if (typeof questionCode !== 'string' || (op !== 'eq' && op !== 'ne' && op !== 'in')) {
      return { conditions: [], unsupported: rule }
    }
    conditions.push({
      // Holds the referenced code until toEditorSections resolves it to an editor id below.
      id: nextId('condition'),
      questionId: questionCode,
      op,
      values: Array.isArray(value) ? value.map(String) : [String(value ?? '')],
    })
  }
  return { conditions, unsupported: null }
}

export function toEditorSections(version: AdminSurveyVersionDto): EditorSection[] {
  const sections = version.sections.map((section) => ({
    id: nextId('section'),
    code: section.code,
    codeLocked: true,
    title: section.title,
    description: section.description ?? '',
    questions: section.questions.map(toEditorQuestion),
  }))

  // Second pass: display rules arrive referencing question codes; the editor works in ids.
  const idByCode = new Map<string, string>()
  for (const section of sections) {
    for (const question of section.questions) idByCode.set(question.code, question.id)
  }
  for (const section of sections) {
    for (const question of section.questions) {
      const resolved = question.conditions.filter((condition) => idByCode.has(condition.questionId))
      if (resolved.length !== question.conditions.length) {
        // A rule pointing outside this version cannot be represented; keep it verbatim so saving
        // does not quietly discard it.
        question.unsupportedDisplayRule = { all: question.conditions.map((condition) => ({
          questionCode: condition.questionId,
          op: condition.op,
          value: condition.op === 'in' ? condition.values : condition.values[0],
        })) }
        question.conditions = []
      } else {
        question.conditions = resolved.map((condition) => ({
          ...condition,
          questionId: idByCode.get(condition.questionId) as string,
        }))
      }
    }
  }

  return sections
}

// --- new items -----------------------------------------------------------

export function newSection(index: number): EditorSection {
  return {
    id: nextId('section'),
    code: '',
    codeLocked: false,
    title: `Section ${index + 1}`,
    description: '',
    questions: [],
  }
}

export function newQuestion(): EditorQuestion {
  return {
    id: nextId('question'),
    code: '',
    codeLocked: false,
    type: 'SINGLE_CHOICE',
    prompt: '',
    helpText: '',
    required: false,
    min: '1',
    max: '5',
    unit: '',
    maxLength: '',
    scaleMinLabel: '',
    scaleMaxLabel: '',
    configExtra: {},
    conditions: [],
    unsupportedDisplayRule: null,
    options: [newOption(), newOption()],
  }
}

export function newOption(): EditorOption {
  return {
    id: nextId('option'),
    code: '',
    codeLocked: false,
    label: '',
    score: '',
    signals: '',
    metadataExtra: {},
  }
}

export function newCondition(): EditorCondition {
  return { id: nextId('condition'), questionId: '', op: 'eq', values: [''] }
}

// --- saving --------------------------------------------------------------

function buildConfig(question: EditorQuestion): Record<string, unknown> | null {
  const config: Record<string, unknown> = { ...question.configExtra }

  const min = question.min.trim() === '' ? null : Number(question.min)
  const max = question.max.trim() === '' ? null : Number(question.max)
  const maxLength = question.maxLength.trim() === '' ? null : Number(question.maxLength)

  if (question.type === 'SCALE' || question.type === 'NUMBER') {
    if (min !== null && Number.isFinite(min)) config.min = min
    if (max !== null && Number.isFinite(max)) config.max = max
  }
  if (question.type === 'NUMBER' && question.unit.trim()) {
    config.unit = question.unit.trim()
  }
  if ((question.type === 'TEXT' || question.type === 'LONG_TEXT') && maxLength !== null && Number.isFinite(maxLength)) {
    config.maxLength = maxLength
  }
  if (question.type === 'SCALE' && (question.scaleMinLabel.trim() || question.scaleMaxLabel.trim())) {
    const labels: Record<string, string> = {}
    if (question.scaleMinLabel.trim() && min !== null) labels[String(min)] = question.scaleMinLabel.trim()
    if (question.scaleMaxLabel.trim() && max !== null) labels[String(max)] = question.scaleMaxLabel.trim()
    if (Object.keys(labels).length > 0) config.scaleLabels = labels
  }

  return Object.keys(config).length === 0 ? null : config
}

function buildDisplayRule(
  question: EditorQuestion,
  codes: AssignedCodes,
  typesByQuestionId: Map<string, QuestionType>,
): Record<string, unknown> | null {
  if (question.unsupportedDisplayRule) return question.unsupportedDisplayRule

  const usable = question.conditions.filter(
    (condition) =>
      codes.questions.has(condition.questionId) && condition.values.some((value) => value !== ''),
  )
  if (usable.length === 0) return null

  return {
    all: usable.map((condition) => {
      const referencedType = typesByQuestionId.get(condition.questionId)
      const cast = (value: string): string | number =>
        referencedType === 'NUMBER' || referencedType === 'SCALE' ? Number(value) : value

      return {
        questionCode: codes.questions.get(condition.questionId) as string,
        op: condition.op,
        value:
          condition.op === 'in'
            ? condition.values.filter((value) => value !== '').map(cast)
            : cast(condition.values[0] ?? ''),
      }
    }),
  }
}

function buildMetadata(option: EditorOption): Record<string, unknown> | null {
  const metadata: Record<string, unknown> = { ...option.metadataExtra }
  const signals = option.signals
    .split(',')
    .map((signal) => signal.trim().toUpperCase())
    .filter(Boolean)
  if (signals.length > 0) metadata.signals = signals
  return Object.keys(metadata).length === 0 ? null : metadata
}

/**
 * Converts the editor into the API shape. Sort order is positional - the list on screen is the
 * order - and every code comes from {@link assignCodes}.
 */
export function toSectionDtos(sections: EditorSection[]): AdminSectionDto[] {
  const codes = assignCodes(sections)

  const typesByQuestionId = new Map<string, QuestionType>()
  for (const section of sections) {
    for (const question of section.questions) typesByQuestionId.set(question.id, question.type)
  }

  return sections.map((section, sectionIndex) => ({
    code: codes.sections.get(section.id) as string,
    title: section.title.trim(),
    description: section.description.trim() || null,
    sortOrder: sectionIndex,
    questions: section.questions.map((question, questionIndex) => ({
      code: codes.questions.get(question.id) as string,
      type: question.type,
      prompt: question.prompt.trim(),
      helpText: question.helpText.trim() || null,
      required: question.required,
      sortOrder: questionIndex,
      config: buildConfig(question),
      displayRule: buildDisplayRule(question, codes, typesByQuestionId),
      options: isChoiceType(question.type)
        ? question.options.map((option, optionIndex) => ({
            code: codes.options.get(option.id) as string,
            label: option.label.trim(),
            sortOrder: optionIndex,
            valueNumeric: option.score.trim() === '' ? null : Number(option.score),
            metadata: buildMetadata(option),
          }))
        : [],
    })),
  }))
}

export function move<T>(items: T[], from: number, to: number): T[] {
  if (to < 0 || to >= items.length) return items
  const next = [...items]
  const [moved] = next.splice(from, 1)
  next.splice(to, 0, moved)
  return next
}
