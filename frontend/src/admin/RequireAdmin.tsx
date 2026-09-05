import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { FullPageSpinner } from '../components/ui/Spinner'
import { useMe } from '../profile/hooks'

/**
 * Hides the admin area from non-admins. This is a convenience, not the control: authority comes
 * from {@code app_user.role} and every admin route is gated server-side by ROLE_ADMIN.
 */
export function RequireAdmin({ children }: { children: ReactNode }) {
  const { data: me, isLoading, error } = useMe()

  if (isLoading) return <FullPageSpinner />
  if (error || me?.role !== 'ADMIN') return <Navigate to="/" replace />
  return <>{children}</>
}
