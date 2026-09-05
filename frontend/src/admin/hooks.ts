import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import * as api from './api'
import { productsQueryKey } from '../store/hooks'
import type {
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

export const adminSurveysKey = ['admin', 'surveys'] as const
export const adminProductsKey = ['admin', 'products'] as const
export const adminOrdersKey = (status: OrderStatus | null) => ['admin', 'orders', status] as const
export const adminUsersKey = (status: UserStatus | null) => ['admin', 'users', status] as const
export const adminVersionKey = (surveyId: string, versionId: string) =>
  ['admin', 'surveys', surveyId, 'versions', versionId] as const

// --- surveys ---

export function useAdminSurveys() {
  return useQuery({ queryKey: adminSurveysKey, queryFn: api.listSurveys })
}

export function useCreateSurvey() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: CreateSurveyRequest) => api.createSurvey(body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: adminSurveysKey }),
  })
}

export function useUpdateSurvey() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ surveyId, body }: { surveyId: string; body: UpdateSurveyRequest }) =>
      api.updateSurvey(surveyId, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminSurveysKey })
      // Retiring a survey changes what the storefront lists.
      queryClient.invalidateQueries({ queryKey: ['surveys'] })
    },
  })
}

export function useCreateDraft() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (surveyId: string) => api.createDraft(surveyId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: adminSurveysKey }),
  })
}

export function useAdminVersion(surveyId: string | undefined, versionId: string | undefined) {
  return useQuery({
    queryKey: adminVersionKey(surveyId ?? '', versionId ?? ''),
    queryFn: () => api.getVersion(surveyId as string, versionId as string),
    enabled: !!surveyId && !!versionId,
  })
}

export function useSaveVersion(surveyId: string, versionId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: SaveVersionContentRequest) => api.saveVersion(surveyId, versionId, body),
    onSuccess: (data) => {
      queryClient.setQueryData(adminVersionKey(surveyId, versionId), data)
      queryClient.invalidateQueries({ queryKey: adminSurveysKey })
    },
  })
}

export function usePublishVersion(surveyId: string, versionId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => api.publishVersion(surveyId, versionId),
    onSuccess: (data) => {
      queryClient.setQueryData(adminVersionKey(surveyId, versionId), data)
      queryClient.invalidateQueries({ queryKey: adminSurveysKey })
      // A newly published version is what users are served next.
      queryClient.invalidateQueries({ queryKey: ['surveys'] })
    },
  })
}

export function useDeleteDraft() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ surveyId, versionId }: { surveyId: string; versionId: string }) =>
      api.deleteDraft(surveyId, versionId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: adminSurveysKey }),
  })
}

// --- products ---

export function useAdminProducts() {
  return useQuery({ queryKey: adminProductsKey, queryFn: api.listAdminProducts })
}

function invalidateCatalog(queryClient: ReturnType<typeof useQueryClient>) {
  queryClient.invalidateQueries({ queryKey: adminProductsKey })
  queryClient.invalidateQueries({ queryKey: productsQueryKey })
}

export function useCreateProduct() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: CreateProductRequest) => api.createProduct(body),
    onSuccess: () => invalidateCatalog(queryClient),
  })
}

export function useUpdateProduct() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ productId, body }: { productId: string; body: UpdateProductRequest }) =>
      api.updateProduct(productId, body),
    onSuccess: () => invalidateCatalog(queryClient),
  })
}

export function useDeactivateProduct() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (productId: string) => api.deactivateProduct(productId),
    onSuccess: () => invalidateCatalog(queryClient),
  })
}

// --- orders ---

export function useAdminOrders(status: OrderStatus | null) {
  return useQuery({ queryKey: adminOrdersKey(status), queryFn: () => api.listAdminOrders(status) })
}

export function useFulfillOrder() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ orderId, body }: { orderId: string; body: FulfillOrderRequest }) =>
      api.fulfillOrder(orderId, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'orders'] })
      queryClient.invalidateQueries({ queryKey: ['store', 'orders'] })
      // Shipping draws stock down, so both catalog views are now stale.
      invalidateCatalog(queryClient)
    },
  })
}

// --- users ---

export function useAdminUsers(status: UserStatus | null) {
  return useQuery({ queryKey: adminUsersKey(status), queryFn: () => api.listAdminUsers(status) })
}

export function useUpdateUserStatus() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ userId, body }: { userId: string; body: UpdateUserStatusRequest }) =>
      api.updateUserStatus(userId, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'users'] }),
  })
}
