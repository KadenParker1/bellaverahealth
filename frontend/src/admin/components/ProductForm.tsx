import { useState, type FormEvent } from 'react'
import { Button } from '../../components/ui/Button'
import { ErrorBanner } from '../../components/ui/ErrorBanner'
import type { AdminProductDto } from '../../types/api'

export interface ProductFormValues {
  code: string
  name: string
  description: string
  imageUrl: string
  priceCents: number
  currency: string
  stripePriceId: string
  sortOrder: number
  /** null means "do not track stock for this product". */
  stockQuantity: number | null
}

/** Money is entered as a decimal and stored as minor units; this is the only conversion point. */
function centsToInput(cents: number): string {
  return (cents / 100).toFixed(2)
}

function inputToCents(value: string): number {
  const parsed = Number.parseFloat(value)
  return Number.isFinite(parsed) ? Math.round(parsed * 100) : 0
}

export function ProductForm({
  product,
  submitLabel,
  pending,
  error,
  onSubmit,
  onCancel,
}: {
  product?: AdminProductDto
  submitLabel: string
  pending: boolean
  error: unknown
  onSubmit: (values: ProductFormValues) => void
  onCancel?: () => void
}) {
  const [code, setCode] = useState(product?.code ?? '')
  const [name, setName] = useState(product?.name ?? '')
  const [description, setDescription] = useState(product?.description ?? '')
  const [imageUrl, setImageUrl] = useState(product?.imageUrl ?? '')
  const [price, setPrice] = useState(centsToInput(product?.priceCents ?? 0))
  const [currency, setCurrency] = useState(product?.currency ?? 'usd')
  const [stripePriceId, setStripePriceId] = useState(product?.stripePriceId ?? '')
  const [sortOrder, setSortOrder] = useState(String(product?.sortOrder ?? 0))
  const [stock, setStock] = useState(
    product?.stockQuantity === null || product?.stockQuantity === undefined
      ? ''
      : String(product.stockQuantity),
  )

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    onSubmit({
      code: code.trim(),
      name: name.trim(),
      description: description.trim(),
      imageUrl: imageUrl.trim(),
      priceCents: inputToCents(price),
      currency: currency.trim().toLowerCase(),
      stripePriceId: stripePriceId.trim(),
      sortOrder: Number.parseInt(sortOrder, 10) || 0,
      stockQuantity: stock.trim() === '' ? null : Math.max(0, Number.parseInt(stock, 10) || 0),
    })
  }

  const field = 'w-full rounded-lg border border-surface-border bg-white px-3 py-2 text-sm text-ink outline-none focus:border-magenta-500'
  const label = 'block text-sm'
  const labelText = 'mb-1 block text-xs font-medium text-ink-muted'

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {error ? <ErrorBanner error={error} /> : null}

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <label className={label}>
          <span className={labelText}>Code</span>
          <input
            value={code}
            onChange={(event) => setCode(event.target.value)}
            disabled={!!product}
            required
            placeholder="iron-support"
            className={`${field} disabled:bg-surface-subtle disabled:text-ink-muted`}
          />
          {product && (
            <span className="mt-1 block text-xs text-ink-muted">
              Fixed — past order lines reference it.
            </span>
          )}
        </label>

        <label className={label}>
          <span className={labelText}>Name</span>
          <input value={name} onChange={(event) => setName(event.target.value)} required className={field} />
        </label>
      </div>

      <label className={label}>
        <span className={labelText}>Description</span>
        <textarea
          value={description}
          onChange={(event) => setDescription(event.target.value)}
          rows={3}
          className={field}
        />
      </label>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <label className={label}>
          <span className={labelText}>Price</span>
          <input
            type="number"
            step="0.01"
            min="0"
            value={price}
            onChange={(event) => setPrice(event.target.value)}
            required
            className={field}
          />
        </label>
        <label className={label}>
          <span className={labelText}>Currency</span>
          <input value={currency} onChange={(event) => setCurrency(event.target.value)} className={field} />
        </label>
        <label className={label}>
          <span className={labelText}>Sort order</span>
          <input
            type="number"
            value={sortOrder}
            onChange={(event) => setSortOrder(event.target.value)}
            className={field}
          />
        </label>
      </div>

      <label className={label}>
        <span className={labelText}>Stock on hand</span>
        <input
          type="number"
          min="0"
          value={stock}
          onChange={(event) => setStock(event.target.value)}
          placeholder="leave blank for unlimited"
          className={field}
        />
        <span className="mt-1 block text-xs text-ink-muted">
          Units in hand. Drawn down when an order ships, not when it's bought — the storefront sells
          against this number minus whatever paid orders are still waiting to be packed. Blank means
          the product isn't stock-tracked.
          {product?.available !== null && product?.available !== undefined && (
            <> Currently {product.available} of {product.stockQuantity} sellable.</>
          )}
        </span>
      </label>

      <label className={label}>
        <span className={labelText}>Image URL</span>
        <input value={imageUrl} onChange={(event) => setImageUrl(event.target.value)} className={field} />
      </label>

      <label className={label}>
        <span className={labelText}>Stripe price ID (optional)</span>
        <input
          value={stripePriceId}
          onChange={(event) => setStripePriceId(event.target.value)}
          placeholder="price_..."
          className={field}
        />
        <span className="mt-1 block text-xs text-ink-muted">
          Set this once the catalog is mirrored in Stripe — checkout then uses the registered price
          instead of sending the amount inline.
        </span>
      </label>

      <div className="flex gap-3">
        <Button type="submit" disabled={pending}>
          {pending ? 'Saving…' : submitLabel}
        </Button>
        {onCancel && (
          <Button type="button" variant="secondary" onClick={onCancel}>
            Cancel
          </Button>
        )}
      </div>
    </form>
  )
}
