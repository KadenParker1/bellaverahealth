import { apiClient } from '../lib/apiClient'
import type { SendContactMessageRequest } from '../types/api'

export const sendContactMessage = (body: SendContactMessageRequest) =>
  apiClient.post<void>('/contact', body)
