import type { OrderStatus } from '../../types/api'

const STYLES: Record<OrderStatus, string> = {
  PENDING: 'bg-amber-100 text-amber-800',
  PAID: 'bg-sky-100 text-sky-800',
  FULFILLED: 'bg-emerald-100 text-emerald-800',
  CANCELLED: 'bg-neutral-200 text-neutral-700',
}

const LABELS: Record<OrderStatus, string> = {
  PENDING: 'Awaiting payment',
  PAID: 'Paid — being packed',
  FULFILLED: 'Shipped',
  CANCELLED: 'Cancelled',
}

export function OrderStatusBadge({ status }: { status: OrderStatus }) {
  return (
    <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${STYLES[status]}`}>
      {LABELS[status]}
    </span>
  )
}
