import { useState } from 'react'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { ErrorBanner } from '../../components/ui/ErrorBanner'
import { Spinner } from '../../components/ui/Spinner'
import { OrderStatusBadge } from '../../store/components/OrderStatusBadge'
import { formatMoney } from '../../store/money'
import { useAdminOrders, useFulfillOrder } from '../hooks'
import type { AdminOrderDto, OrderStatus } from '../../types/api'

interface OrderView {
  key: string
  label: string
  status: OrderStatus | null
  blurb: string
  empty: string
}

const VIEWS: OrderView[] = [
  {
    key: 'queue',
    label: 'To ship',
    status: 'PAID',
    blurb: 'Paid and not yet shipped, oldest first. Pack these.',
    empty: 'Nothing to ship. The queue is clear.',
  },
  {
    key: 'shipped',
    label: 'Shipped',
    status: 'FULFILLED',
    blurb: 'Already on their way, most recently shipped first.',
    empty: 'Nothing has shipped yet.',
  },
  {
    key: 'all',
    label: 'All orders',
    status: null,
    blurb: 'Every order including unpaid and cancelled, newest first.',
    empty: 'No orders yet.',
  },
]

/** The shipping queue and the order history behind it. */
const ORDERS_PER_PAGE = 20

export function AdminOrdersPage() {
  const [viewKey, setViewKey] = useState(VIEWS[0].key)
  const [page, setPage] = useState(0)
  const view = VIEWS.find((candidate) => candidate.key === viewKey) ?? VIEWS[0]
  const { data, isLoading, error } = useAdminOrders(view.status, page, ORDERS_PER_PAGE)
  const orders = data?.content

  // Switching tab has to reset the page, or "To ship" can land on a page that doesn't exist there.
  const selectView = (key: string) => {
    setViewKey(key)
    setPage(0)
  }

  return (
    <div>
      <div className="mb-6">
        <div className="mb-3 flex flex-wrap gap-2">
          {VIEWS.map((candidate) => (
            <button
              key={candidate.key}
              type="button"
              onClick={() => selectView(candidate.key)}
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

      {orders && orders.length === 0 && (
        <Card className="p-10 text-center">
          <p className="text-sm text-ink-muted">{view.empty}</p>
        </Card>
      )}

      <div className="space-y-4">
        {orders?.map((order) => (
          <AdminOrderCard key={order.id} order={order} />
        ))}
      </div>

      {data && data.totalPages > 1 && (
        <div className="mt-6 flex items-center justify-center gap-3">
          <Button variant="ghost" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
            Previous
          </Button>
          <span className="text-sm text-ink-muted">
            Page {page + 1} of {data.totalPages} · {data.totalElements} order
            {data.totalElements === 1 ? '' : 's'}
          </span>
          <Button
            variant="ghost"
            disabled={page + 1 >= data.totalPages}
            onClick={() => setPage((p) => p + 1)}
          >
            Next
          </Button>
        </div>
      )}
    </div>
  )
}

function AdminOrderCard({ order }: { order: AdminOrderDto }) {
  const [carrier, setCarrier] = useState('')
  const [trackingNumber, setTrackingNumber] = useState('')
  const fulfill = useFulfillOrder()

  return (
    <Card className="p-6">
      <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-sm font-semibold text-ink">Order {order.id.slice(0, 8)}</p>
          <p className="text-xs text-ink-muted">
            {order.customerEmail ?? 'no email on file'} · placed{' '}
            {new Date(order.placedAt).toLocaleString()}
          </p>
        </div>
        <OrderStatusBadge status={order.status} />
      </div>

      <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
        <div>
          <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-ink-muted">Pack</p>
          <ul className="space-y-1 text-sm text-ink">
            {order.items.map((item) => (
              <li key={item.productCode} className="flex justify-between gap-4">
                <span>
                  <span className="font-medium">{item.quantity} ×</span> {item.productName}
                </span>
                <span className="text-ink-muted">{formatMoney(item.lineTotalCents, order.currency)}</span>
              </li>
            ))}
          </ul>
          <p className="mt-2 border-t border-surface-border pt-2 text-sm font-semibold text-ink">
            {formatMoney(order.subtotalCents, order.currency)}
          </p>
        </div>

        <div>
          <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-ink-muted">Ship to</p>
          {order.shipTo?.line1 ? (
            <address className="text-sm not-italic text-ink">
              {order.shipTo.name && <div>{order.shipTo.name}</div>}
              <div>{order.shipTo.line1}</div>
              {order.shipTo.line2 && <div>{order.shipTo.line2}</div>}
              <div>
                {[order.shipTo.city, order.shipTo.region, order.shipTo.postalCode]
                  .filter(Boolean)
                  .join(', ')}
              </div>
              {order.shipTo.country && <div>{order.shipTo.country}</div>}
            </address>
          ) : (
            <p className="text-sm text-ink-muted">No shipping address was collected.</p>
          )}
        </div>
      </div>

      {order.status === 'PAID' && (
        <div className="mt-6 border-t border-surface-border pt-4">
          {fulfill.error ? (
            <div className="mb-3">
              <ErrorBanner error={fulfill.error} />
            </div>
          ) : null}
          <div className="flex flex-wrap items-end gap-3">
            <label className="text-sm">
              <span className="mb-1 block text-xs text-ink-muted">Carrier (optional)</span>
              <input
                value={carrier}
                onChange={(event) => setCarrier(event.target.value)}
                className="w-40 rounded-lg border border-surface-border px-3 py-2 text-sm text-ink"
              />
            </label>
            <label className="text-sm">
              <span className="mb-1 block text-xs text-ink-muted">Tracking number (optional)</span>
              <input
                value={trackingNumber}
                onChange={(event) => setTrackingNumber(event.target.value)}
                className="w-56 rounded-lg border border-surface-border px-3 py-2 text-sm text-ink"
              />
            </label>
            <Button
              disabled={fulfill.isPending}
              onClick={() =>
                fulfill.mutate({
                  orderId: order.id,
                  body: { carrier: carrier || undefined, trackingNumber: trackingNumber || undefined },
                })
              }
            >
              {fulfill.isPending ? 'Marking…' : 'Mark fulfilled'}
            </Button>
          </div>
        </div>
      )}

      {order.status === 'FULFILLED' && (
        <p className="mt-4 border-t border-surface-border pt-4 text-xs text-ink-muted">
          Fulfilled {order.fulfilledAt ? new Date(order.fulfilledAt).toLocaleString() : ''}
          {order.carrier ? ` via ${order.carrier}` : ''}
          {order.trackingNumber ? ` — ${order.trackingNumber}` : ''}
        </p>
      )}
    </Card>
  )
}
