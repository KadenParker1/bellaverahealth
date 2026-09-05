import type { OrderDto } from '../../types/api'
import { formatMoney } from '../money'
import { OrderStatusBadge } from './OrderStatusBadge'

export function OrderSummary({ order }: { order: OrderDto }) {
  return (
    <div>
      <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
        <div>
          <p className="text-sm font-semibold text-ink">Order {order.id.slice(0, 8)}</p>
          <p className="text-xs text-ink-muted">Placed {new Date(order.placedAt).toLocaleDateString()}</p>
        </div>
        <OrderStatusBadge status={order.status} />
      </div>

      <ul className="mb-3 divide-y divide-surface-border border-y border-surface-border">
        {order.items.map((item) => (
          <li key={item.productCode} className="flex items-center justify-between py-2 text-sm">
            <span className="text-ink">
              {item.productName}
              <span className="text-ink-muted"> × {item.quantity}</span>
            </span>
            <span className="text-ink">{formatMoney(item.lineTotalCents, order.currency)}</span>
          </li>
        ))}
      </ul>

      <div className="flex items-center justify-between text-sm font-semibold text-ink">
        <span>Total</span>
        <span>{formatMoney(order.subtotalCents, order.currency)}</span>
      </div>

      {order.status === 'FULFILLED' && (
        <p className="mt-3 text-xs text-ink-muted">
          Shipped {order.fulfilledAt ? new Date(order.fulfilledAt).toLocaleDateString() : ''}
          {order.carrier ? ` via ${order.carrier}` : ''}
          {order.trackingNumber ? ` — tracking ${order.trackingNumber}` : ''}
        </p>
      )}

      {order.shipTo?.line1 && (
        <p className="mt-3 text-xs text-ink-muted">
          Ships to {order.shipTo.name ? `${order.shipTo.name}, ` : ''}
          {[order.shipTo.line1, order.shipTo.city, order.shipTo.region, order.shipTo.postalCode]
            .filter(Boolean)
            .join(', ')}
        </p>
      )}
    </div>
  )
}
