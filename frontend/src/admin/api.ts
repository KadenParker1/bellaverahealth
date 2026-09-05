import { apiClient } from '../lib/apiClient'
import type {
  AdminOrderDto,
  AdminUserDto,
  AdminProductDto,
  AdminSurveyDto,
  AdminSurveyVersionDto,
  CreateProductRequest,
  CreateSurveyRequest,
  FulfillOrderRequest,
  OrderStatus,
  SaveVersionContentRequest,
  UpdateProductRequest,
  UpdateSurveyRequest,
  UpdateUserStatusRequest,
  UserStatus,
} from '../types/api'

// --- surveys ---

export const listSurveys = () => apiClient.get<AdminSurveyDto[]>('/admin/surveys')

export const createSurvey = (body: CreateSurveyRequest) =>
  apiClient.post<AdminSurveyDto>('/admin/surveys', body)

export const updateSurvey = (surveyId: string, body: UpdateSurveyRequest) =>
  apiClient.patch<AdminSurveyDto>(`/admin/surveys/${surveyId}`, body)

export const createDraft = (surveyId: string) =>
  apiClient.post<AdminSurveyVersionDto>(`/admin/surveys/${surveyId}/versions`)

export const getVersion = (surveyId: string, versionId: string) =>
  apiClient.get<AdminSurveyVersionDto>(`/admin/surveys/${surveyId}/versions/${versionId}`)

export const saveVersion = (surveyId: string, versionId: string, body: SaveVersionContentRequest) =>
  apiClient.put<AdminSurveyVersionDto>(`/admin/surveys/${surveyId}/versions/${versionId}`, body)

export const publishVersion = (surveyId: string, versionId: string) =>
  apiClient.post<AdminSurveyVersionDto>(`/admin/surveys/${surveyId}/versions/${versionId}/publish`)

export const deleteDraft = (surveyId: string, versionId: string) =>
  apiClient.del<void>(`/admin/surveys/${surveyId}/versions/${versionId}`)

// --- products ---

export const listAdminProducts = () => apiClient.get<AdminProductDto[]>('/admin/products')

export const createProduct = (body: CreateProductRequest) =>
  apiClient.post<AdminProductDto>('/admin/products', body)

export const updateProduct = (productId: string, body: UpdateProductRequest) =>
  apiClient.patch<AdminProductDto>(`/admin/products/${productId}`, body)

/** Deactivates rather than destroys - order lines still reference the product. */
export const deactivateProduct = (productId: string) =>
  apiClient.del<AdminProductDto>(`/admin/products/${productId}`)

// --- orders ---

/** No status means the full history, newest first. `PAID` is the packing queue, oldest first. */
export const listAdminOrders = (status: OrderStatus | null) =>
  apiClient.get<AdminOrderDto[]>(`/admin/orders${status ? `?status=${status}` : ''}`)

export const fulfillOrder = (orderId: string, body: FulfillOrderRequest) =>
  apiClient.post<AdminOrderDto>(`/admin/orders/${orderId}/fulfill`, body)

// --- users ---

/** No status means every account. `SUSPENDED` lists just the banned ones. */
export const listAdminUsers = (status: UserStatus | null) =>
  apiClient.get<AdminUserDto[]>(`/admin/users${status ? `?status=${status}` : ''}`)

/** Ban with `SUSPENDED`, reinstate with `ACTIVE`. */
export const updateUserStatus = (userId: string, body: UpdateUserStatusRequest) =>
  apiClient.patch<AdminUserDto>(`/admin/users/${userId}`, body)
