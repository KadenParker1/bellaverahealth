import { useEffect, useRef } from 'react'
import { useMyThread, useSendMessage, useThreadMessages } from '../hooks'
import { ChatMessageBubble } from '../components/ChatMessageBubble'
import { ChatComposer } from '../components/ChatComposer'
import { Card } from '../../components/ui/Card'
import { Spinner } from '../../components/ui/Spinner'
import { ErrorBanner } from '../../components/ui/ErrorBanner'

export function ChatPage() {
  const { thread, isLoading: loadingThread, error: threadError } = useMyThread()
  const { data: messages, isLoading: loadingMessages } = useThreadMessages(thread?.id)
  const sendMessage = useSendMessage(thread?.id)
  const bottomRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  if (loadingThread) {
    return (
      <div className="flex justify-center py-16">
        <Spinner className="h-8 w-8" />
      </div>
    )
  }
  if (threadError) return <ErrorBanner error={threadError} />

  return (
    <div className="mx-auto max-w-2xl">
      <h1 className="mb-6 text-2xl font-bold text-ink">Chat</h1>
      <Card className="flex h-[60vh] flex-col overflow-hidden">
        <div className="flex-1 space-y-3 overflow-y-auto p-4">
          {loadingMessages && (
            <div className="flex justify-center py-8">
              <Spinner />
            </div>
          )}
          {!loadingMessages && (!messages || messages.length === 0) && (
            <p className="py-8 text-center text-sm text-ink-muted">
              Ask anything about your surveys and results.
            </p>
          )}
          {messages?.map((message) => (
            <ChatMessageBubble key={message.id} message={message} />
          ))}
          {sendMessage.error ? <ErrorBanner error={sendMessage.error} /> : null}
          <div ref={bottomRef} />
        </div>
        <ChatComposer
          sending={sendMessage.isPending}
          onSend={(message) => sendMessage.mutate(message)}
        />
      </Card>
    </div>
  )
}
