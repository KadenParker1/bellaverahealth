import { useState } from 'react'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { ErrorBanner } from '../../components/ui/ErrorBanner'
import { Spinner } from '../../components/ui/Spinner'
import { useMe } from '../../profile/hooks'
import { useAdminUsers, useUpdateUserStatus } from '../hooks'
import type { AdminUserDto, UserStatus } from '../../types/api'

interface UserView {
  key: string
  label: string
  status: UserStatus | null
  blurb: string
  empty: string
}

const VIEWS: UserView[] = [
  {
    key: 'all',
    label: 'All accounts',
    status: null,
    blurb: 'Everyone who has ever signed in, by email.',
    empty: 'No accounts yet.',
  },
  {
    key: 'banned',
    label: 'Banned',
    status: 'SUSPENDED',
    blurb: 'Suspended accounts. Every request from these is refused until they are reinstated.',
    empty: 'Nobody is banned.',
  },
]

export function AdminUsersPage() {
  const [viewKey, setViewKey] = useState(VIEWS[0].key)
  const view = VIEWS.find((candidate) => candidate.key === viewKey) ?? VIEWS[0]
  const { data: users, isLoading, error } = useAdminUsers(view.status)
  const { data: me } = useMe()

  return (
    <div>
      <div className="mb-6">
        <div className="mb-3 flex flex-wrap gap-2">
          {VIEWS.map((candidate) => (
            <button
              key={candidate.key}
              type="button"
              onClick={() => setViewKey(candidate.key)}
              className={[
                'rounded-full px-4 py-1.5 text-sm font-semibold transition-colors',
                candidate.key === view.key
                  ? 'bg-magenta-500 text-white'
                  : 'bg-surface text-ink-muted hover:text-ink',
              ].join(' ')}
            >
              {candidate.label}
            </button>
          ))}
        </div>
        <p className="text-sm text-ink-muted">{view.blurb}</p>
      </div>

      {error ? <ErrorBanner error={error} /> : null}
      {isLoading && (
        <div className="flex justify-center py-16">
          <Spinner className="h-8 w-8" />
        </div>
      )}

      {users && users.length === 0 && (
        <Card className="p-10 text-center">
          <p className="text-sm text-ink-muted">{view.empty}</p>
        </Card>
      )}

      <div className="space-y-3">
        {users?.map((user) => (
          <AdminUserRow key={user.userId} user={user} isSelf={user.userId === me?.userId} />
        ))}
      </div>
    </div>
  )
}

const STATUS_STYLES: Record<UserStatus, string> = {
  ACTIVE: 'bg-green-100 text-green-800',
  SUSPENDED: 'bg-red-100 text-red-800',
  DELETED: 'bg-surface-subtle text-ink-muted',
}

function StatusBadge({ status }: { status: UserStatus }) {
  return (
    <span
      className={`rounded-full px-2.5 py-1 text-xs font-semibold ${STATUS_STYLES[status]}`}
    >
      {status === 'SUSPENDED' ? 'BANNED' : status}
    </span>
  )
}

function AdminUserRow({ user, isSelf }: { user: AdminUserDto; isSelf: boolean }) {
  const [confirming, setConfirming] = useState(false)
  const [reason, setReason] = useState('')
  const updateStatus = useUpdateUserStatus()

  const banned = user.status !== 'ACTIVE'

  function submit(status: UserStatus) {
    updateStatus.mutate(
      { userId: user.userId, body: { status, reason: reason || undefined } },
      {
        onSuccess: () => {
          setConfirming(false)
          setReason('')
        },
      },
    )
  }

  return (
    <Card className="p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="truncate text-sm font-semibold text-ink">
            {user.displayName ?? user.email}
            {user.role === 'ADMIN' && (
              <span className="ml-2 rounded-full bg-magenta-100 px-2 py-0.5 text-xs font-semibold text-magenta-700">
                ADMIN
              </span>
            )}
          </p>
          <p className="truncate text-xs text-ink-muted">
            {user.displayName ? `${user.email} · ` : ''}
            joined {new Date(user.createdAt).toLocaleDateString()}
            {user.onboardingCompletedAt ? '' : ' · onboarding incomplete'}
          </p>
        </div>

        <div className="flex items-center gap-3">
          <StatusBadge status={user.status} />
          {isSelf ? (
            <span className="text-xs text-ink-muted">That's you</span>
          ) : banned ? (
            <Button
              variant="secondary"
              disabled={updateStatus.isPending}
              onClick={() => submit('ACTIVE')}
            >
              {updateStatus.isPending ? 'Working…' : 'Reinstate'}
            </Button>
          ) : (
            <Button
              variant="secondary"
              disabled={updateStatus.isPending}
              onClick={() => setConfirming((open) => !open)}
            >
              Ban
            </Button>
          )}
        </div>
      </div>

      {updateStatus.error ? (
        <div className="mt-3">
          <ErrorBanner error={updateStatus.error} />
        </div>
      ) : null}

      {confirming && !banned && (
        <div className="mt-4 border-t border-surface-border pt-4">
          <p className="mb-3 text-sm text-ink">
            Banning refuses every request from this account from their next one onward. Their
            answers, orders, and chat history are kept.
          </p>
          <div className="flex flex-wrap items-end gap-3">
            <label className="flex-1 text-sm">
              <span className="mb-1 block text-xs text-ink-muted">Reason (recorded in the audit log)</span>
              <input
                value={reason}
                onChange={(event) => setReason(event.target.value)}
                maxLength={500}
                className="w-full rounded-lg border border-surface-border px-3 py-2 text-sm text-ink"
              />
            </label>
            <Button disabled={updateStatus.isPending} onClick={() => submit('SUSPENDED')}>
              {updateStatus.isPending ? 'Banning…' : 'Confirm ban'}
            </Button>
            <Button variant="ghost" onClick={() => setConfirming(false)}>
              Cancel
            </Button>
          </div>
        </div>
      )}
    </Card>
  )
}
