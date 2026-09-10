import { apiClient } from '../lib/apiClient'
import type { CheckoutRequest, CheckoutSessionDto, OrderDto, PageResponse, ProductDto } from '../types/api'

export const listProducts = () => apiClient.get<ProductDto[]>('/store/products')

export const getProduct = (code: string) => apiClient.get<ProductDto>(`/store/products/${code}`)

export const startCheckout = (body: CheckoutRequest) =>
  apiClient.post<CheckoutSessionDto>('/store/checkout', body)

/** @param page 0-based. */
export const listMyOrders = (page: number, size: number) =>
  apiClient.get<PageResponse<OrderDto>>(`/store/orders/me?page=${page}&size=${size}`)

export const getOrder = (orderId: string) => apiClient.get<OrderDto>(`/store/orders/${orderId}`)
