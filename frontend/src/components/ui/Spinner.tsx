export function Spinner({ className = '' }: { className?: string }) {
  return (
    <div
      role="status"
      aria-label="Loading"
      className={`h-6 w-6 animate-spin rounded-full border-2 border-surface-border border-t-magenta-500 ${className}`}
    />
  )
}

export function FullPageSpinner() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-surface-subtle">
      <Spinner className="h-8 w-8" />
    </div>
  )
}
