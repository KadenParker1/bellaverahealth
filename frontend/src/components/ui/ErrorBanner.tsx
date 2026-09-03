import { ApiError } from '../../lib/apiClient'

export function ErrorBanner({ error }: { error: unknown }) {
  if (!error) return null

  const message = error instanceof ApiError ? error.message : (error as Error).message
  const details = error instanceof ApiError ? error.errors : undefined

  return (
    <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700" role="alert">
      <p className="font-medium">{message || 'Something went wrong.'}</p>
      {details && details.length > 0 && (
        <ul className="mt-1 list-inside list-disc">
          {details.map((detail) => (
            <li key={detail}>{detail}</li>
          ))}
        </ul>
      )}
    </div>
  )
}
