import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { ErrorBanner } from '../../components/ui/ErrorBanner'
import { Spinner } from '../../components/ui/Spinner'
import { useBlogPosts } from '../hooks'

export function BlogPage() {
  const [page, setPage] = useState(0)
  const { data, isLoading, error } = useBlogPosts(page, 10)

  return (
    <div className="mx-auto max-w-2xl">
      <h1 className="mb-2 text-2xl font-bold text-ink">Blog</h1>
      <p className="mb-8 text-sm text-ink-muted">Notes, research, and updates from the team.</p>

      {error ? <ErrorBanner error={error} /> : null}
      {isLoading && (
        <div className="flex justify-center py-16">
          <Spinner className="h-8 w-8" />
        </div>
      )}

      {data && data.content.length === 0 && (
        <Card className="p-10 text-center">
          <p className="text-sm text-ink-muted">Nothing published yet.</p>
        </Card>
      )}

      <div className="space-y-4">
        {data?.content.map((post) => (
          <Link key={post.slug} to={`/blog/${post.slug}`}>
            <Card className="p-6 transition-shadow hover:shadow-md">
              <p className="text-base font-semibold text-ink">{post.title}</p>
              {post.publishedAt && (
                <p className="mt-0.5 text-xs text-ink-muted">
                  {new Date(post.publishedAt).toLocaleDateString()}
                </p>
              )}
              {post.excerpt && <p className="mt-2 text-sm text-ink-muted">{post.excerpt}</p>}
            </Card>
          </Link>
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
