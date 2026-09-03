import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-surface-subtle text-center">
      <h1 className="text-2xl font-bold text-ink">Page not found</h1>
      <Link to="/" className="text-sm font-medium text-magenta-600 hover:underline">
        Go home
      </Link>
    </div>
  )
}
