import { Link } from 'react-router-dom'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { ErrorBanner } from '../../components/ui/ErrorBanner'
import { Spinner } from '../../components/ui/Spinner'
import { useCart } from '../CartContext'
import { useProducts } from '../hooks'
import { formatMoney } from '../money'
import type { ProductDto } from '../../types/api'

export function StorePage() {
  const { data: products, isLoading, error } = useProducts()
  const { add, itemCount } = useCart()

  return (
    <div>
      <div className="mb-8 flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="mb-1 text-2xl font-bold text-ink">Store</h1>
          <p className="text-sm text-ink-muted">Products chosen to sit alongside your plan.</p>
        </div>
        <Link to="/store/cart">
          <Button variant="secondary">Cart{itemCount > 0 ? ` (${itemCount})` : ''}</Button>
        </Link>
      </div>

      {isLoading && (
        <div className="flex justify-center py-16">
          <Spinner className="h-8 w-8" />
        </div>
      )}
      {error ? <ErrorBanner error={error} /> : null}

      {products && products.length === 0 && (
        <Card className="p-10 text-center">
          <p className="text-sm text-ink-muted">
            Nothing in the shop yet. An admin can add products from the admin console.
          </p>
        </Card>
      )}

      {products && products.length > 0 && (
        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {products.map((product) => (
            <ProductCard key={product.id} product={product} onAdd={() => add(product.code)} />
          ))}
        </div>
      )}
    </div>
  )
}

function ProductCard({ product, onAdd }: { product: ProductDto; onAdd: () => void }) {
  // Untracked stock arrives as an absent field (Jackson omits nulls), so normalize before compare.
  const available = product.available ?? null
  const soldOut = available !== null && available <= 0

  return (
    <Card className="flex flex-col overflow-hidden">
      {product.imageUrl && (
        <img src={product.imageUrl} alt="" className="h-44 w-full object-cover" loading="lazy" />
      )}
      <div className="flex flex-1 flex-col p-5">
        <h2 className="text-base font-semibold text-ink">{product.name}</h2>
        {product.description && (
          <p className="mt-1 flex-1 text-sm text-ink-muted">{product.description}</p>
        )}
        <div className="mt-4 flex items-center justify-between">
          <span className="text-lg font-bold text-ink">
            {formatMoney(product.priceCents, product.currency)}
          </span>
          <Button disabled={soldOut} onClick={onAdd}>
            {soldOut ? 'Out of stock' : 'Add to cart'}
          </Button>
        </div>
        {available !== null && available > 0 && available <= 5 && (
          <p className="mt-2 text-xs font-medium text-magenta-600">Only {available} left</p>
        )}
      </div>
    </Card>
  )
}
