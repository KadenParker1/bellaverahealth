import { useQuery } from '@tanstack/react-query'
import { getBlogPost, listBlogPosts } from './api'

export const blogPostsKey = (page: number, size: number) => ['blog', 'posts', page, size] as const

export function useBlogPosts(page: number, size: number) {
  return useQuery({ queryKey: blogPostsKey(page, size), queryFn: () => listBlogPosts(page, size) })
}

export function useBlogPost(slug: string | undefined) {
  return useQuery({
    queryKey: ['blog', 'posts', slug],
    queryFn: () => getBlogPost(slug as string),
    enabled: !!slug,
  })
}
