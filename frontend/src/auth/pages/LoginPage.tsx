import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../useAuth'
import { Card } from '../../components/ui/Card'
import { TextField } from '../../components/ui/TextField'
import { Button } from '../../components/ui/Button'
import { ErrorBanner } from '../../components/ui/ErrorBanner'

export function LoginPage() {
  const { signInWithPassword } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const from = (location.state as { from?: string } | null)?.from ?? '/'

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    const { error: signInError } = await signInWithPassword(email, password)
    setSubmitting(false)
    if (signInError) {
      setError(signInError.message)
      return
    }
    navigate(from, { replace: true })
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-surface-subtle px-6">
      <Card className="w-full max-w-sm p-8">
        <h1 className="mb-1 text-xl font-bold text-ink">Welcome back</h1>
        <p className="mb-6 text-sm text-ink-muted">Sign in to continue to Bellavera.</p>
        <form onSubmit={handleSubmit} className="space-y-4">
          <TextField
            label="Email"
            type="email"
            name="email"
            autoComplete="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
          <TextField
            label="Password"
            type="password"
            name="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          {error && <ErrorBanner error={new Error(error)} />}
          <Button type="submit" className="w-full" disabled={submitting}>
            {submitting ? 'Signing in...' : 'Sign in'}
          </Button>
        </form>
        <p className="mt-6 text-center text-sm text-ink-muted">
          Don't have an account?{' '}
          <Link to="/signup" className="font-medium text-magenta-600 hover:underline">
            Sign up
          </Link>
        </p>
      </Card>
    </div>
  )
}
