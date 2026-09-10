import { useState } from 'react'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { ErrorBanner } from '../../components/ui/ErrorBanner'
import { Spinner } from '../../components/ui/Spinner'
import { useContactMessages, useMarkContactMessageRead } from '../hooks'
import type { AdminContactMessageDto } from '../../types/api'

/**
 * There is no in-app reply - this hands off to whatever mail client the admin's OS/browser has
 * configured, addressed to the sender's own account email (never a free-text field they typed).
 */
function replyMailto(message: AdminContactMessageDto): string {
  const subject = `Re: ${message.subject}`
  const quoted = message.message
    .split('\n')
    .map((line) => `> ${line}`)
    .join('\n')
  const body = `\n\n\n---\nOn ${new Date(message.createdAt).toLocaleString()}, you wrote:\n${quoted}`
  return `mailto:${message.senderEmail}?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`
}

export function AdminMessagesPage() {
  const [page, setPage] = useState(0)
  const { data, isLoading, error } = useContactMessages(page, 20)
  const markRead = useMarkContactMessageRead()

  return (
    <div>
      <div className="mb-6">
        <h2 className="text-lg font-semibold text-ink">Messages</h2>
        <p className="text-sm text-ink-muted">Sent through the Contact page, newest first.</p>
      </div>

      {error ? <ErrorBanner error={error} /> : null}
      {isLoading && (
        <div className="flex justify-center py-16">
          <Spinner className="h-8 w-8" />
        </div>
      )}

      {data && data.content.length === 0 && (
        <Card className="p-10 text-center">
          <p className="text-sm text-ink-muted">No messages yet.</p>
        </Card>
      )}

      <div className="space-y-3">
        {data?.content.map((message) => (
          <Card key={message.id} className="p-5">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div className="min-w-0">
                <p className="truncate text-sm font-semibold text-ink">
                  {message.subject}
                  {!message.readAt && (
                    <span className="ml-2 rounded-full bg-magenta-100 px-2 py-0.5 text-xs font-semibold text-magenta-700">
                      New
                    </span>
                  )}
                </p>
                <p className="text-xs text-ink-muted">
                  {message.senderEmail} · {new Date(message.createdAt).toLocaleString()}
                </p>
              </div>
              <div className="flex shrink-0 gap-2">
                <a href={replyMailto(message)}>
                  <Button variant="secondary">Reply by email</Button>
                </a>
                {!message.readAt && (
                  <Button
                    variant="secondary"
                    disabled={markRead.isPending}
                    onClick={() => markRead.mutate(message.id)}
                  >
                    Mark read
                  </Button>
                )}
              </div>
            </div>
            <p className="mt-3 whitespace-pre-wrap text-sm text-ink">{message.message}</p>
          </Card>
        ))}
      </div>

      {data && data.totalPages > 1 && (
        <div className="mt-6 flex items-center justify-center gap-3">
          <Button variant="ghost" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
            Previous
          </Button>
          <span className="text-sm text-ink-muted">
            Page {page + 1} of {data.totalPages}
          </span>
          <Button variant="ghost" disabled={page + 1 >= data.totalPages} onClick={() => setPage((p) => p + 1)}>
            Next
          </Button>
        </div>
      )}
    </div>
  )
}
