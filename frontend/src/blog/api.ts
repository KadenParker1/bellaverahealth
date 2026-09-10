import { apiClient } from '../lib/apiClient'
import type { BlogPostDto, BlogPostSummaryDto, PageResponse } from '../types/api'

/** @param page 0-based. */
export const listBlogPosts = (page: number, size: number) =>
  apiClient.get<PageResponse<BlogPostSummaryDto>>(`/blog?page=${page}&size=${size}`)

export const getBlogPost = (slug: string) => apiClient.get<BlogPostDto>(`/blog/${slug}`)
