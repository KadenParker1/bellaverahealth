import { apiClient } from '../lib/apiClient'
import type { UpdateProfileRequest, UserProfileResponse } from '../types/api'

export const getMe = () => apiClient.get<UserProfileResponse>('/me')

export const updateMe = (body: UpdateProfileRequest) =>
  apiClient.patch<UserProfileResponse>('/me', body)
