import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../useAuth'
import { Card } from '../../components/ui/Card'
import { TextField } from '../../components/ui/TextField'
import { Button } from '../../components/ui/Button'
import { ErrorBanner } from '../../components/ui/ErrorBanner'

export function SignupPage() {
  const { signUp } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [confirmationSent, setConfirmationSent] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)

    if (password !== confirmPassword) {
      setError('Passwords do not match.')
      return
    }

    setSubmitting(true)
    const { data, error: signUpError } = await signUp(email, password)
    setSubmitting(false)

    if (signUpError) {
      setError(signUpError.message)
      return
    }

    if (data.session) {
      // Local dev / confirmations disabled: session comes back immediately.
      navigate('/onboarding', { replace: true })
      return
    }

    if (data.user) {
      // Prod path: email confirmation required before a session is issued.
      setConfirmationSent(true)
      return
    }

    setError('Something went wrong creating your account. Please try again.')
  }

  if (confirmationSent) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-surface-subtle px-6">
        <Card className="w-full max-w-sm p-8 text-center">
          <h1 className="mb-2 text-xl font-bold text-ink">Check your email</h1>
          <p className="text-sm text-ink-muted">
            We sent a confirmation link to <strong>{email}</strong>. Confirm your email to finish
            creating your account.
          </p>
        </Card>
      </div>
    )
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-surface-subtle px-6">
      <Card className="w-full max-w-sm p-8">
        <h1 className="mb-1 text-xl font-bold text-ink">Create your account</h1>
        <p className="mb-6 text-sm text-ink-muted">Start your personalized health journey.</p>
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
            autoComplete="new-password"
            required
            minLength={6}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          <TextField
            label="Confirm password"
            type="password"
            name="confirmPassword"
            autoComplete="new-password"
            required
            minLength={6}
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
          />
          {error && <ErrorBanner error={new Error(error)} />}
          <Button type="submit" className="w-full" disabled={submitting}>
            {submitting ? 'Creating account...' : 'Sign up'}
          </Button>
        </form>
        <p className="mt-6 text-center text-sm text-ink-muted">
          Already have an account?{' '}
          <Link to="/login" className="font-medium text-magenta-600 hover:underline">
            Sign in
          </Link>
        </p>
      </Card>
    </div>
  )
}
