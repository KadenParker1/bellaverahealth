import { Outlet } from 'react-router-dom'
import { RequireAuth } from '../../auth/RequireAuth'
import { RequireOnboarding } from '../../auth/RequireOnboarding'
import { AppShell } from './AppShell'

export function ProtectedLayout() {
  return (
    <RequireAuth>
      <RequireOnboarding>
        <AppShell>
          <Outlet />
        </AppShell>
      </RequireOnboarding>
    </RequireAuth>
  )
}
