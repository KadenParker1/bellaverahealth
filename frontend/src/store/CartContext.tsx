import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'

export interface CartLine {
  productCode: string
  quantity: number
}

interface CartContextValue {
  lines: CartLine[]
  itemCount: number
  add: (productCode: string, quantity?: number) => void
  setQuantity: (productCode: string, quantity: number) => void
  remove: (productCode: string) => void
  clear: () => void
}

const CartContext = createContext<CartContextValue | undefined>(undefined)

const STORAGE_KEY = 'bellavera.cart'

/**
 * The cart is per-browser and holds codes and quantities only - never prices. What a basket costs
 * is decided server-side at checkout, so nothing here is worth tampering with.
 */
function readStoredCart(): CartLine[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return []
    const parsed: unknown = JSON.parse(raw)
    if (!Array.isArray(parsed)) return []
    return parsed.flatMap((line) => {
      if (typeof line !== 'object' || line === null) return []
      const { productCode, quantity } = line as Partial<CartLine>
      if (typeof productCode !== 'string' || typeof quantity !== 'number' || quantity < 1) return []
      return [{ productCode, quantity: Math.floor(quantity) }]
    })
  } catch {
    // Private windows and cleared site data both land here; an empty cart is the right answer.
    return []
  }
}

export function CartProvider({ children }: { children: ReactNode }) {
  const [lines, setLines] = useState<CartLine[]>(readStoredCart)

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(lines))
    } catch {
      // Storage being unavailable must not break the page; the cart just won't survive a reload.
    }
  }, [lines])

  const add = useCallback((productCode: string, quantity = 1) => {
    setLines((current) => {
      const existing = current.find((line) => line.productCode === productCode)
      if (!existing) return [...current, { productCode, quantity }]
      return current.map((line) =>
        line.productCode === productCode ? { ...line, quantity: line.quantity + quantity } : line,
      )
    })
  }, [])

  const setQuantity = useCallback((productCode: string, quantity: number) => {
    setLines((current) =>
      quantity < 1
        ? current.filter((line) => line.productCode !== productCode)
        : current.map((line) => (line.productCode === productCode ? { ...line, quantity } : line)),
    )
  }, [])

  const remove = useCallback((productCode: string) => {
    setLines((current) => current.filter((line) => line.productCode !== productCode))
  }, [])

  const clear = useCallback(() => setLines([]), [])

  const value = useMemo<CartContextValue>(
    () => ({
      lines,
      itemCount: lines.reduce((total, line) => total + line.quantity, 0),
      add,
      setQuantity,
      remove,
      clear,
    }),
    [lines, add, setQuantity, remove, clear],
  )

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>
}

export function useCart() {
  const ctx = useContext(CartContext)
  if (!ctx) throw new Error('useCart must be used within a CartProvider')
  return ctx
}
