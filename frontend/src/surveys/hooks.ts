import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getActiveSurveys, getMyResponse, getSurveyDetail, submitResponse } from './api'
import type { SubmitResponseRequest } from '../types/api'
import { meQueryKey } from '../profile/hooks'

export const activeSurveysKey = ['surveys', 'active'] as const

export function useActiveSurveys() {
  return useQuery({ queryKey: activeSurveysKey, queryFn: getActiveSurveys })
}

export function useSurveyDetail(surveyId: string | undefined) {
  return useQuery({
    queryKey: ['surveys', surveyId],
    queryFn: () => getSurveyDetail(surveyId as string),
    enabled: !!surveyId,
  })
}

export function useMyResponse(surveyId: string | undefined) {
  return useQuery({
    queryKey: ['surveys', surveyId, 'responses', 'me'],
    queryFn: () => getMyResponse(surveyId as string),
    enabled: !!surveyId,
  })
}

export function useSubmitResponse(surveyId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: SubmitResponseRequest) => submitResponse(surveyId, body),
    onSuccess: () =>
      // Returned so mutateAsync callers wait for these refetches to land - callers that
      // navigate to a route guarded by RequireOnboarding right after submitting need the
      // fresh `me` data in cache first, or the guard bounces them straight back.
      //
      // `me` gets `refetchQueries(..., type: 'all')` rather than `invalidateQueries` because
      // nothing observes useMe() while sitting on /onboarding (only RequireOnboarding does,
      // and it isn't mounted here) - invalidateQueries only refetches *actively observed*
      // queries by default, so it would just mark the cache stale and resolve immediately
      // without ever calling the network. RequireOnboarding would then mount on the next
      // route, read that stale cached (pre-submit) data synchronously, and redirect straight
      // back to /onboarding before the real refetch finished. `refetchQueries` forces the
      // network call regardless of active observers, so the cache is genuinely fresh by the
      // time this promise resolves.
      Promise.all([
        queryClient.invalidateQueries({ queryKey: ['surveys', surveyId, 'responses', 'me'] }),
        queryClient.invalidateQueries({ queryKey: activeSurveysKey }),
        queryClient.refetchQueries({ queryKey: meQueryKey, type: 'all' }),
      ]),
  })
}
