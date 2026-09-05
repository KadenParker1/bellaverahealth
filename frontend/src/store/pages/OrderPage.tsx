import { useEffect } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { ErrorBanner } from '../../components/ui/ErrorBanner'
import { Spinner } from '../../components/ui/Spinner'
import { useCart } from '../CartContext'
import { useOrder } from '../hooks'
import { OrderSummary } from '../components/OrderSummary'

/**
 * Where checkout returns to. Landing here means the customer finished the provider's flow - it is
 * not proof of payment, so the page reports whatever status the order actually has and polls until
 * the payment webhook lands.
 */
export function OrderPage() {
  const [searchParams] = useSearchParams()
  const orderId = searchParams.get('orderId')
  const { data: order, isLoading, error } = useOrder(orderId)
  const { clear } = useCart()

  useEffect(() => {
    if (orderId) clear()
  }, [orderId, clear])

  return (
    <div className="mx-auto max-w-2xl">
      <h1 className="mb-2 text-2xl font-bold text-ink">Thanks for your order</h1>
      <p className="mb-6 text-sm text-ink-muted">
        We'll email you when it ships. You can always find it under My Account.
      </p>

      {!orderId && <ErrorBanner error={new Error('No order was referenced in this link.')} />}
      {isLoading && (
        <div className="flex justify-center py-16">
          <Spinner className="h-8 w-8" />
        </div>
      )}
      {error ? <ErrorBanner error={error} /> : null}

      {order && (
        <Card className="p-6">
          <OrderSummary order={order} />
          {order.status === 'PENDING' && (
            <p className="mt-4 rounded-lg bg-surface-subtle px-3 py-2 text-xs text-ink-muted">
              Waiting for the payment to be confirmed. This page updates itself.
            </p>
          )}
        </Card>
      )}

      <div className="mt-6 flex gap-3">
        <Link to="/store">
          <Button variant="secondary">Back to the store</Button>
        </Link>
        <Link to="/account">
          <Button variant="ghost">My orders</Button>
        </Link>
      </div>
    </div>
  )
}
