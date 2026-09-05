/**
 * Prices travel as minor units, so formatting is the only place they become decimal.
 * Intl handles the zero-decimal currencies (JPY and friends) that a naive /100 would mangle.
 */
export function formatMoney(cents: number, currency: string): string {
  return new Intl.NumberFormat(undefined, {
    style: 'currency',
    currency: currency.toUpperCase(),
  }).format(cents / 100)
}
