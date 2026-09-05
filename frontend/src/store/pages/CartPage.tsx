import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { ErrorBanner } from '../../components/ui/ErrorBanner'
import { Spinner } from '../../components/ui/Spinner'
import { useCart } from '../CartContext'
import { useProducts, useStartCheckout } from '../hooks'
import { formatMoney } from '../money'

export function CartPage() {
  const { lines, setQuantity, remove } = useCart()
  const { data: products, isLoading } = useProducts()
  const checkout = useStartCheckout()
  const [checkoutError, setCheckoutError] = useState<unknown>(null)

  const productsByCode = new Map((products ?? []).map((product) => [product.code, product]))
  // A code that has left the catalog since it went in the cart: show it, don't price it.
  const rows = lines.map((line) => ({ line, product: productsByCode.get(line.productCode) }))
  const currency = products?.[0]?.currency ?? 'usd'
  const subtotal = rows.reduce(
    (total, { line, product }) => total + (product ? product.priceCents * line.quantity : 0),
    0,
  )
  const hasUnavailable = rows.some((row) => !row.product)
  // The server is the authority - this just says so before the checkout call bounces.
  // `available` is absent, not null, when a product is untracked (Jackson omits nulls).
  const overStock = rows.some(({ line, product }) => {
    const available = product?.available ?? null
    return available !== null && line.quantity > available
  })

  const onCheckout = async () => {
    setCheckoutError(null)
    try {
      const session = await checkout.mutateAsync({
        items: lines.map((line) => ({ productCode: line.productCode, quantity: line.quantity })),
      })
      // Leaves the SPA for the provider's hosted page. The cart survives a cancelled checkout
      // because the cancel URL comes back here.
      window.location.href = session.checkoutUrl
    } catch (error) {
      setCheckoutError(error)
    }
  }

  return (
    <div className="mx-auto max-w-2xl">
      <h1 className="mb-6 text-2xl font-bold text-ink">Your cart</h1>

      {isLoading && (
        <div className="flex justify-center py-16">
          <Spinner className="h-8 w-8" />
        </div>
      )}

      {!isLoading && lines.length === 0 && (
        <Card className="p-10 text-center">
          <p className="mb-4 text-sm text-ink-muted">Your cart is empty.</p>
          <Link to="/store">
            <Button variant="secondary">Browse the store</Button>
          </Link>
        </Card>
      )}

      {!isLoading && lines.length > 0 && (
        <Card className="p-6">
          <ul className="divide-y divide-surface-border">
            {rows.map(({ line, product }) => (
              <li key={line.productCode} className="flex items-center gap-4 py-4">
                <div className="flex-1">
                  <p className="text-sm font-semibold text-ink">{product?.name ?? line.productCode}</p>
                  {product ? (
                    <>
                      <p className="text-xs text-ink-muted">
                        {formatMoney(product.priceCents, product.currency)} each
                      </p>
                      {(product.available ?? null) !== null &&
                        line.quantity > (product.available as number) && (
                          <p className="text-xs font-medium text-red-600">
                            {product.available === 0
                              ? 'Out of stock — remove it to check out.'
                              : `Only ${product.available} left — reduce the quantity to check out.`}
                          </p>
                        )}
                    </>
                  ) : (
                    <p className="text-xs text-red-600">No longer available — remove it to check out.</p>
                  )}
                </div>
                <input
                  type="number"
                  min={1}
                  value={line.quantity}
                  onChange={(event) => setQuantity(line.productCode, Number(event.target.value))}
                  aria-label={`Quantity for ${product?.name ?? line.productCode}`}
                  className="w-16 rounded-lg border border-surface-border px-2 py-1.5 text-sm text-ink"
                />
                <span className="w-20 text-right text-sm font-medium text-ink">
                  {product ? formatMoney(product.priceCents * line.quantity, product.currency) : '—'}
                </span>
                <button
                  type="button"
                  onClick={() => remove(line.productCode)}
                  className="text-sm text-ink-muted hover:text-magenta-600"
                >
                  Remove
                </button>
              </li>
            ))}
          </ul>

          <div className="mt-4 flex items-center justify-between border-t border-surface-border pt-4">
            <span className="text-sm font-semibold text-ink">Subtotal</span>
            <span className="text-lg font-bold text-ink">{formatMoney(subtotal, currency)}</span>
          </div>
          <p className="mt-1 text-xs text-ink-muted">
            Shipping and tax are calculated at checkout. The total charged is worked out on the
            server from the current catalog price.
          </p>

          {checkoutError ? (
            <div className="mt-4">
              <ErrorBanner error={checkoutError} />
            </div>
          ) : null}

          <Button
            className="mt-4 w-full"
            disabled={checkout.isPending || hasUnavailable || overStock}
            onClick={onCheckout}
          >
            {checkout.isPending ? 'Starting checkout…' : 'Checkout'}
          </Button>
        </Card>
      )}
    </div>
  )
}
