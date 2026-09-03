import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getThreadMessages, getThreads, postChat } from './api'

const threadsKey = ['chat', 'threads'] as const
const messagesKey = (threadId: string) => ['chat', 'threads', threadId, 'messages'] as const

// Chat is a single ongoing thread per user - "the" thread is the most recently active one.
export function useMyThread() {
  const query = useQuery({ queryKey: threadsKey, queryFn: getThreads })
  const thread = query.data?.[0]
  return { ...query, thread }
}

export function useThreadMessages(threadId: string | undefined) {
  return useQuery({
    queryKey: threadId ? messagesKey(threadId) : ['chat', 'threads', 'none'],
    queryFn: () => getThreadMessages(threadId as string),
    enabled: !!threadId,
  })
}

export function useSendMessage(threadId: string | undefined) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (message: string) => postChat({ threadId, message }),
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: threadsKey })
      queryClient.invalidateQueries({ queryKey: messagesKey(response.threadId) })
    },
  })
}
