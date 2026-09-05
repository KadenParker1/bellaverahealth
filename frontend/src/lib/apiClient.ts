import { supabase } from './supabaseClient'
import type { ProblemDetail } from '../types/api'

export class ApiError extends Error {
  status: number
  title?: string
  errors?: string[]

  constructor(problem: ProblemDetail, status: number) {
    super(problem.detail ?? problem.title ?? `Request failed with status ${status}`)
    this.status = status
    this.title = problem.title
    this.errors = problem.errors
  }
}

// TODO(prod): once the frontend and backend are split across Vercel/Railway,
// swap this for `import.meta.env.VITE_API_BASE_URL ?? '/api/v1'` and configure
// a Vercel rewrite (or the env var) accordingly. Local dev proxies /api via vite.config.ts.
const BASE_URL = '/api/v1'

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const {
    data: { session },
  } = await supabase.auth.getSession()

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(init.headers as Record<string, string> | undefined),
  }
  if (session?.access_token) {
    headers.Authorization = `Bearer ${session.access_token}`
  }

  const res = await fetch(`${BASE_URL}${path}`, { ...init, headers })

  if (!res.ok) {
    let problem: ProblemDetail = {}
    try {
      problem = await res.json()
    } catch {
      // non-JSON error body, fall through with an empty ProblemDetail
    }
    throw new ApiError(problem, res.status)
  }

  if (res.status === 204) {
    return undefined as T
  }
  return res.json() as Promise<T>
}

export const apiClient = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'POST', body: body !== undefined ? JSON.stringify(body) : undefined }),
  patch: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'PATCH', body: body !== undefined ? JSON.stringify(body) : undefined }),
  put: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'PUT', body: body !== undefined ? JSON.stringify(body) : undefined }),
  del: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
}
