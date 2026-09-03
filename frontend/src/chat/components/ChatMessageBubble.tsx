import type { ChatMessageDto } from '../../types/api'

export function ChatMessageBubble({ message }: { message: ChatMessageDto }) {
  const isUser = message.role === 'USER'
  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'}`}>
      <div
        className={`max-w-[75%] rounded-2xl px-4 py-2.5 text-sm ${
          isUser ? 'bg-magenta-500 text-white' : 'border border-surface-border bg-white text-ink'
        }`}
      >
        {message.content}
      </div>
    </div>
  )
}
