import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../auth/useAuth'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { ErrorBanner } from '../../components/ui/ErrorBanner'
import { Spinner } from '../../components/ui/Spinner'
import { useMe, useUpdateMe } from '../../profile/hooks'
import { OrderSummary } from '../../store/components/OrderSummary'
import { useMyOrders } from '../../store/hooks'

const ORDERS_PER_PAGE = 5

export function MyAccountPage() {
  const { signOut } = useAuth()
  const { data: me, isLoading: meLoading, error: meError } = useMe()
  const [ordersPage, setOrdersPage] = useState(0)
  const {
    data: ordersPageResult,
    isLoading: ordersLoading,
    error: ordersError,
  } = useMyOrders(ordersPage, ORDERS_PER_PAGE)
  const orders = ordersPageResult?.content
  const updateMe = useUpdateMe()

  return (
    <div className="mx-auto max-w-3xl">
      <h1 className="mb-6 text-2xl font-bold text-ink">My account</h1>

      {meError ? <ErrorBanner error={meError} /> : null}
      {meLoading && (
        <div className="flex justify-center py-8">
          <Spinner className="h-8 w-8" />
        </div>
      )}

      {me && (
        <Card className="mb-8 p-6">
          <h2 className="mb-4 text-base font-semibold text-ink">Profile</h2>
          <dl className="grid grid-cols-1 gap-3 text-sm sm:grid-cols-2">
            <div>
              <dt className="text-ink-muted">Name</dt>
              <dd className="text-ink">{me.displayName ?? '—'}</dd>
            </div>
            <div>
              <dt className="text-ink-muted">Email</dt>
              <dd className="text-ink">{me.email}</dd>
            </div>
            <div>
              <dt className="text-ink-muted">Units</dt>
              <dd className="text-ink">{me.unitSystem === 'METRIC' ? 'Metric' : 'Imperial'}</dd>
            </div>
            <div>
              <dt className="text-ink-muted">Member since</dt>
              <dd className="text-ink">
                {me.onboardingCompletedAt
                  ? new Date(me.onboardingCompletedAt).toLocaleDateString()
                  : '—'}
              </dd>
            </div>
          </dl>

          <label className="mt-6 flex items-center gap-2 text-sm text-ink">
            <input
              type="checkbox"
              checked={me.emailOptIn}
              disabled={updateMe.isPending}
              onChange={(event) => updateMe.mutate({ emailOptIn: event.target.checked })}
              className="h-4 w-4 rounded border-surface-border"
            />
            Send me the email chain
          </label>
          {updateMe.error ? (
            <div className="mt-3">
              <ErrorBanner error={updateMe.error} />
            </div>
          ) : null}

          <div className="mt-6 flex flex-wrap gap-3">
            {me.role === 'ADMIN' && (
              <Link to="/admin">
                <Button variant="secondary">Admin console</Button>
              </Link>
            )}
            <Button variant="ghost" onClick={() => signOut()}>
              Sign out
            </Button>
          </div>
        </Card>
      )}

      <h2 className="mb-4 text-base font-semibold text-ink">Orders</h2>
      {ordersError ? <ErrorBanner error={ordersError} /> : null}
      {ordersLoading && (
        <div className="flex justify-center py-8">
          <Spinner className="h-8 w-8" />
        </div>
      )}
      {orders && orders.length === 0 && (
        <Card className="p-8 text-center">
          <p className="mb-4 text-sm text-ink-muted">You haven't ordered anything yet.</p>
          <Link to="/store">
            <Button variant="secondary">Visit the store</Button>
          </Link>
        </Card>
      )}
      {orders && orders.length > 0 && (
        <div className="space-y-4">
          {orders.map((order) => (
            <Card key={order.id} className="p-6">
              <OrderSummary order={order} />
            </Card>
          ))}
        </div>
      )}

      {ordersPageResult && ordersPageResult.totalPages > 1 && (
        <div className="mt-4 flex items-center justify-center gap-3">
          <Button
            variant="ghost"
            disabled={ordersPage === 0}
            onClick={() => setOrdersPage((page) => page - 1)}
          >
            Previous
          </Button>
          <span className="text-sm text-ink-muted">
            Page {ordersPage + 1} of {ordersPageResult.totalPages}
          </span>
          <Button
            variant="ghost"
            disabled={ordersPage + 1 >= ordersPageResult.totalPages}
            onClick={() => setOrdersPage((page) => page + 1)}
          >
            Next
          </Button>
        </div>
      )}
    </div>
  )
}
