import { useState, type FormEvent } from 'react'
import { Button } from '../../components/ui/Button'

export function ChatComposer({
  onSend,
  sending,
}: {
  onSend: (message: string) => void
  sending: boolean
}) {
  const [message, setMessage] = useState('')

  function handleSubmit(e: FormEvent) {
    e.preventDefault()
    const trimmed = message.trim()
    if (!trimmed) return
    onSend(trimmed)
    setMessage('')
  }

  return (
    <form onSubmit={handleSubmit} className="flex gap-2 border-t border-surface-border p-4">
      <input
        type="text"
        value={message}
        onChange={(e) => setMessage(e.target.value)}
        placeholder="Ask a question..."
        className="flex-1 rounded-lg border border-surface-border bg-white px-3 py-2.5 text-sm text-ink outline-none focus:border-magenta-500 focus:ring-1 focus:ring-magenta-500"
      />
      <Button type="submit" disabled={sending || !message.trim()}>
        Send
      </Button>
    </form>
  )
}
