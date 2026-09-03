import { apiClient, ApiError } from '../lib/apiClient'
import type {
  SubmitResponseRequest,
  SurveyDetailDto,
  SurveyResponseDetailDto,
  SurveySummaryDto,
} from '../types/api'

export const getActiveSurveys = () => apiClient.get<SurveySummaryDto[]>('/surveys/active')

export const getSurveyDetail = (surveyId: string) =>
  apiClient.get<SurveyDetailDto>(`/surveys/${surveyId}`)

export const submitResponse = (surveyId: string, body: SubmitResponseRequest) =>
  apiClient.post<SurveyResponseDetailDto>(`/surveys/${surveyId}/responses`, body)

export async function getMyResponse(surveyId: string): Promise<SurveyResponseDetailDto | null> {
  try {
    return await apiClient.get<SurveyResponseDetailDto>(`/surveys/${surveyId}/responses/me`)
  } catch (err) {
    if (err instanceof ApiError && err.status === 404) return null
    throw err
  }
}
