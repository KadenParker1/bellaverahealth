import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useMe } from '../profile/hooks'
import { FullPageSpinner } from '../components/ui/Spinner'

export function RequireOnboarding({ children }: { children: ReactNode }) {
  const { data: me, isLoading } = useMe()

  if (isLoading) return <FullPageSpinner />
  if (me && !me.onboardingCompletedAt) {
    return <Navigate to="/onboarding" replace />
  }
  return <>{children}</>
}
