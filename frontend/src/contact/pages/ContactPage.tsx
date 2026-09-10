import { useState } from 'react'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { ErrorBanner } from '../../components/ui/ErrorBanner'
import { useSendContactMessage } from '../hooks'

const FIELD =
  'w-full rounded-lg border border-surface-border bg-white px-3 py-2 text-sm text-ink outline-none focus:border-magenta-500'
const LABEL = 'mb-1 block text-xs font-medium text-ink-muted'

export function ContactPage() {
  const [subject, setSubject] = useState('')
  const [message, setMessage] = useState('')
  const send = useSendContactMessage()

  const submit = () => {
    send.mutate(
      { subject: subject.trim(), message: message.trim() },
      {
        onSuccess: () => {
          setSubject('')
          setMessage('')
        },
      },
    )
  }

  return (
    <div className="mx-auto max-w-2xl">
      <h1 className="mb-2 text-2xl font-bold text-ink">Contact</h1>
      <p className="mb-8 text-sm text-ink-muted">
        Send us a message and we'll get back to you.
      </p>

      <Card className="p-8">
        {send.isSuccess && (
          <p className="mb-4 rounded-lg bg-emerald-50 px-4 py-2 text-sm text-emerald-800">
            Thanks - your message has been sent.
          </p>
        )}
        {send.error ? (
          <div className="mb-4">
            <ErrorBanner error={send.error} />
          </div>
        ) : null}

        <div className="space-y-4">
          <label className="block text-sm">
            <span className={LABEL}>Subject</span>
            <input
              value={subject}
              onChange={(event) => setSubject(event.target.value)}
              maxLength={200}
              className={FIELD}
            />
          </label>
          <label className="block text-sm">
            <span className={LABEL}>Message</span>
            <textarea
              value={message}
              onChange={(event) => setMessage(event.target.value)}
              rows={6}
              maxLength={5000}
              className={FIELD}
            />
          </label>
          <Button disabled={send.isPending || !subject.trim() || !message.trim()} onClick={submit}>
            {send.isPending ? 'Sending…' : 'Send message'}
          </Button>
        </div>
      </Card>
    </div>
  )
}
