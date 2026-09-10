import { Link, useParams } from 'react-router-dom'
import { ErrorBanner } from '../../components/ui/ErrorBanner'
import { Spinner } from '../../components/ui/Spinner'
import { useBlogPost } from '../hooks'

export function BlogPostPage() {
  const { slug } = useParams<{ slug: string }>()
  const { data: post, isLoading, error } = useBlogPost(slug)

  return (
    <div className="mx-auto max-w-2xl">
      <Link to="/blog" className="text-xs font-medium text-magenta-600 hover:underline">
        ← All posts
      </Link>

      {isLoading && (
        <div className="flex justify-center py-16">
          <Spinner className="h-8 w-8" />
        </div>
      )}
      {error ? (
        <div className="mt-4">
          <ErrorBanner error={error} />
        </div>
      ) : null}

      {post && (
        <article className="mt-4">
          <h1 className="mb-1 text-2xl font-bold text-ink">{post.title}</h1>
          {post.publishedAt && (
            <p className="mb-6 text-xs text-ink-muted">
              {new Date(post.publishedAt).toLocaleDateString()}
            </p>
          )}
          <div className="space-y-4 text-sm leading-relaxed text-ink">
            {post.body
              .split(/\n\s*\n/)
              .filter((paragraph) => paragraph.trim() !== '')
              .map((paragraph, index) => (
                <p key={index}>{paragraph.trim()}</p>
              ))}
          </div>
        </article>
      )}
    </div>
  )
}
