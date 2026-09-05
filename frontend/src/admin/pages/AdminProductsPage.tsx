import { useState } from 'react'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { ErrorBanner } from '../../components/ui/ErrorBanner'
import { Spinner } from '../../components/ui/Spinner'
import { formatMoney } from '../../store/money'
import { ProductForm, type ProductFormValues } from '../components/ProductForm'
import { useAdminProducts, useCreateProduct, useDeactivateProduct, useUpdateProduct } from '../hooks'
import type { AdminProductDto } from '../../types/api'

/**
 * Untracked stock arrives as an *absent* field, not null: the API sets
 * `spring.jackson.default-property-inclusion=non_null`. Normalize with `??` before comparing, or
 * `=== null` silently reports every untracked product as "undefined in stock".
 */
function StockLine({ product }: { product: AdminProductDto }) {
  const stock = product.stockQuantity ?? null
  const available = product.available ?? null

  if (stock === null) {
    return <p className="mt-0.5 text-xs text-ink-muted">Stock not tracked</p>
  }

  const outOfStock = available !== null && available <= 0
  const awaitingShipment = available !== null ? stock - available : 0

  return (
    <p className={`mt-0.5 text-xs ${outOfStock ? 'font-medium text-red-600' : 'text-ink-muted'}`}>
      {stock} in stock
      {awaitingShipment > 0 && <> · {available} sellable ({awaitingShipment} awaiting shipment)</>}
      {outOfStock && ' · out of stock'}
    </p>
  )
}

export function AdminProductsPage() {
  const { data: products, isLoading, error } = useAdminProducts()
  const createProduct = useCreateProduct()
  const updateProduct = useUpdateProduct()
  const deactivateProduct = useDeactivateProduct()
  const [creating, setCreating] = useState(false)
  const [editingId, setEditingId] = useState<string | null>(null)

  const onCreate = (values: ProductFormValues) => {
    createProduct.mutate(
      {
        code: values.code,
        name: values.name,
        description: values.description || undefined,
        imageUrl: values.imageUrl || undefined,
        priceCents: values.priceCents,
        currency: values.currency || undefined,
        stripePriceId: values.stripePriceId || undefined,
        sortOrder: values.sortOrder,
        stockQuantity: values.stockQuantity ?? undefined,
      },
      { onSuccess: () => setCreating(false) },
    )
  }

  const onUpdate = (productId: string, values: ProductFormValues) => {
    updateProduct.mutate(
      {
        productId,
        body: {
          name: values.name,
          description: values.description,
          imageUrl: values.imageUrl,
          priceCents: values.priceCents,
          currency: values.currency,
          stripePriceId: values.stripePriceId,
          sortOrder: values.sortOrder,
          // Clearing the field stops tracking; a number sets it. Null alone would be ambiguous.
          ...(values.stockQuantity === null
            ? { clearStock: true }
            : { stockQuantity: values.stockQuantity }),
        },
      },
      { onSuccess: () => setEditingId(null) },
    )
  }

  return (
    <div>
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-ink">Products</h2>
          <p className="text-sm text-ink-muted">
            Removing a product deactivates it — past orders still point at the row.
          </p>
        </div>
        <Button onClick={() => setCreating((current) => !current)}>
          {creating ? 'Close' : 'New product'}
        </Button>
      </div>

      {creating && (
        <Card className="mb-6 p-6">
          <h3 className="mb-4 text-base font-semibold text-ink">New product</h3>
          <ProductForm
            submitLabel="Create product"
            pending={createProduct.isPending}
            error={createProduct.error}
            onSubmit={onCreate}
            onCancel={() => setCreating(false)}
          />
        </Card>
      )}

      {error ? <ErrorBanner error={error} /> : null}
      {isLoading && (
        <div className="flex justify-center py-16">
          <Spinner className="h-8 w-8" />
        </div>
      )}

      {products && products.length === 0 && !creating && (
        <Card className="p-10 text-center">
          <p className="text-sm text-ink-muted">No products yet.</p>
        </Card>
      )}

      <div className="space-y-4">
        {products?.map((product) => (
          <Card key={product.id} className="p-6">
            {editingId === product.id ? (
              <>
                <h3 className="mb-4 text-base font-semibold text-ink">Edit {product.name}</h3>
                <ProductForm
                  product={product}
                  submitLabel="Save changes"
                  pending={updateProduct.isPending}
                  error={updateProduct.error}
                  onSubmit={(values) => onUpdate(product.id, values)}
                  onCancel={() => setEditingId(null)}
                />
              </>
            ) : (
              <div className="flex flex-wrap items-center justify-between gap-4">
                <div className="flex items-center gap-4">
                  {product.imageUrl && (
                    <img src={product.imageUrl} alt="" className="h-14 w-14 rounded-lg object-cover" />
                  )}
                  <div>
                    <p className="text-sm font-semibold text-ink">
                      {product.name}
                      {!product.active && (
                        <span className="ml-2 rounded-full bg-neutral-200 px-2 py-0.5 text-xs font-medium text-neutral-700">
                          Inactive
                        </span>
                      )}
                    </p>
                    <p className="text-xs text-ink-muted">
                      {product.code} · {formatMoney(product.priceCents, product.currency)}
                      {product.stripePriceId ? ` · ${product.stripePriceId}` : ''}
                    </p>
                    <StockLine product={product} />
                  </div>
                </div>
                <div className="flex gap-2">
                  <Button variant="secondary" onClick={() => setEditingId(product.id)}>
                    Edit
                  </Button>
                  {product.active ? (
                    <Button
                      variant="ghost"
                      disabled={deactivateProduct.isPending}
                      onClick={() => deactivateProduct.mutate(product.id)}
                    >
                      Remove from store
                    </Button>
                  ) : (
                    <Button
                      variant="ghost"
                      disabled={updateProduct.isPending}
                      onClick={() =>
                        updateProduct.mutate({ productId: product.id, body: { active: true } })
                      }
                    >
                      Restore
                    </Button>
                  )}
                </div>
              </div>
            )}
          </Card>
        ))}
      </div>

      {deactivateProduct.error ? (
        <div className="mt-4">
          <ErrorBanner error={deactivateProduct.error} />
        </div>
      ) : null}
    </div>
  )
}
