import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getMe, updateMe } from './api'
import { useAuth } from '../auth/useAuth'

export const meQueryKey = ['me'] as const

export function useMe() {
  const { session } = useAuth()
  return useQuery({
    queryKey: meQueryKey,
    queryFn: getMe,
    enabled: !!session,
  })
}

export function useUpdateMe() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: updateMe,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: meQueryKey }),
  })
}
