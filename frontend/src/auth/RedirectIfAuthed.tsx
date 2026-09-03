import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from './useAuth'
import { FullPageSpinner } from '../components/ui/Spinner'

export function RedirectIfAuthed({ children }: { children: ReactNode }) {
  const { session, loading } = useAuth()

  if (loading) return <FullPageSpinner />
  if (session) return <Navigate to="/" replace />
  return <>{children}</>
}
