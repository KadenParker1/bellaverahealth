import { useMutation, useQuery } from '@tanstack/react-query'
import { getOrder, listMyOrders, listProducts, startCheckout } from './api'

export const productsQueryKey = ['store', 'products'] as const
export const myOrdersQueryKey = ['store', 'orders', 'me'] as const

export function useProducts() {
  return useQuery({ queryKey: productsQueryKey, queryFn: listProducts })
}

export function useMyOrders() {
  return useQuery({ queryKey: myOrdersQueryKey, queryFn: listMyOrders })
}

export function useOrder(orderId: string | null) {
  return useQuery({
    queryKey: ['store', 'orders', orderId],
    queryFn: () => getOrder(orderId as string),
    enabled: !!orderId,
    // The order becomes PAID when the payment webhook lands, which is moments after the customer
    // is redirected back - so a freshly-placed order is worth re-reading for a little while.
    refetchInterval: (query) => (query.state.data?.status === 'PENDING' ? 3000 : false),
  })
}

export function useStartCheckout() {
  return useMutation({ mutationFn: startCheckout })
}
