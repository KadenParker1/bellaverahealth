import { apiClient } from '../lib/apiClient'
import type { ChatMessageDto, ChatRequest, ChatResponseDto, ChatThreadSummaryDto } from '../types/api'

export const getThreads = () => apiClient.get<ChatThreadSummaryDto[]>('/chat/threads')

export const getThreadMessages = (threadId: string) =>
  apiClient.get<ChatMessageDto[]>(`/chat/threads/${threadId}`)

export const postChat = (body: ChatRequest) => apiClient.post<ChatResponseDto>('/chat', body)
