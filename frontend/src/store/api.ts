import { apiClient } from '../lib/apiClient'
import type { CheckoutRequest, CheckoutSessionDto, OrderDto, ProductDto } from '../types/api'

export const listProducts = () => apiClient.get<ProductDto[]>('/store/products')

export const getProduct = (code: string) => apiClient.get<ProductDto>(`/store/products/${code}`)

export const startCheckout = (body: CheckoutRequest) =>
  apiClient.post<CheckoutSessionDto>('/store/checkout', body)

export const listMyOrders = () => apiClient.get<OrderDto[]>('/store/orders/me')

export const getOrder = (orderId: string) => apiClient.get<OrderDto>(`/store/orders/${orderId}`)
