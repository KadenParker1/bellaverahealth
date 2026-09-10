import { useState } from 'react'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { ErrorBanner } from '../../components/ui/ErrorBanner'
import { Spinner } from '../../components/ui/Spinner'
import {
  useAdminBlogPosts,
  useCreateBlogPost,
  useDeleteBlogPost,
  useUpdateBlogPost,
} from '../hooks'
import type { AdminBlogPostDto, CreateBlogPostRequest, UpdateBlogPostRequest } from '../../types/api'

const FIELD =
  'w-full rounded-lg border border-surface-border bg-white px-3 py-2 text-sm text-ink outline-none focus:border-magenta-500'
const LABEL = 'mb-1 block text-xs font-medium text-ink-muted'

export function AdminBlogPage() {
  const [page, setPage] = useState(0)
  const { data, isLoading, error } = useAdminBlogPosts(page, 20)
  const createPost = useCreateBlogPost()
  const [creating, setCreating] = useState(false)
  const [editingId, setEditingId] = useState<string | null>(null)

  return (
    <div>
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-ink">Blog</h2>
          <p className="text-sm text-ink-muted">
            Any signed-in user can read a published post. Drafts are visible only here.
          </p>
        </div>
        <Button onClick={() => setCreating((current) => !current)}>
          {creating ? 'Close' : 'New post'}
        </Button>
      </div>

      {creating && (
        <Card className="mb-6 p-6">
          <h3 className="mb-4 text-base font-semibold text-ink">New post</h3>
          <BlogPostForm
            submitLabel="Create draft"
            pending={createPost.isPending}
            error={createPost.error}
            onSubmit={(values) => createPost.mutate(values, { onSuccess: () => setCreating(false) })}
            onCancel={() => setCreating(false)}
          />
        </Card>
      )}

      {error ? <ErrorBanner error={error} /> : null}
      {isLoading && (
        <div className="flex justify-center py-16">
          <Spinner className="h-8 w-8" />
        </div>
      )}

      {data && data.content.length === 0 && !creating && (
        <Card className="p-10 text-center">
          <p className="text-sm text-ink-muted">No posts yet.</p>
        </Card>
      )}

      <div className="space-y-4">
        {data?.content.map((post) => (
          <PostCard
            key={post.id}
            post={post}
            editing={editingId === post.id}
            onEdit={() => setEditingId(post.id)}
            onCancelEdit={() => setEditingId(null)}
          />
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

function PostCard({
  post,
  editing,
  onEdit,
  onCancelEdit,
}: {
  post: AdminBlogPostDto
  editing: boolean
  onEdit: () => void
  onCancelEdit: () => void
}) {
  const updatePost = useUpdateBlogPost()
  const deletePost = useDeleteBlogPost()
  const [confirmingDelete, setConfirmingDelete] = useState(false)

  if (editing) {
    return (
      <Card className="p-6">
        <h3 className="mb-4 text-base font-semibold text-ink">Edit {post.title}</h3>
        <BlogPostForm
          post={post}
          submitLabel="Save changes"
          pending={updatePost.isPending}
          error={updatePost.error}
          onSubmit={(values) => updatePost.mutate({ postId: post.id, body: values }, { onSuccess: onCancelEdit })}
          onCancel={onCancelEdit}
        />
      </Card>
    )
  }

  return (
    <Card className="p-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="min-w-0">
          <p className="truncate text-sm font-semibold text-ink">
            {post.title}
            <span
              className={`ml-2 rounded-full px-2 py-0.5 text-xs font-medium ${
                post.published ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'
              }`}
            >
              {post.published ? 'published' : 'draft'}
            </span>
          </p>
          <p className="truncate text-xs text-ink-muted">
            /{post.slug}
            {post.authorEmail ? ` · ${post.authorEmail}` : ''}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button variant="secondary" onClick={onEdit}>
            Edit
          </Button>
          <Button
            variant="ghost"
            disabled={updatePost.isPending}
            onClick={() =>
              updatePost.mutate({ postId: post.id, body: { published: !post.published } })
            }
          >
            {post.published ? 'Unpublish' : 'Publish'}
          </Button>
          {confirmingDelete ? (
            <>
              <Button
                variant="ghost"
                disabled={deletePost.isPending}
                onClick={() => deletePost.mutate(post.id)}
              >
                {deletePost.isPending ? 'Deleting…' : 'Confirm delete'}
              </Button>
              <Button variant="ghost" onClick={() => setConfirmingDelete(false)}>
                Cancel
              </Button>
            </>
          ) : (
            <Button variant="ghost" onClick={() => setConfirmingDelete(true)}>
              Delete
            </Button>
          )}
        </div>
      </div>
      {deletePost.error ? (
        <div className="mt-3">
          <ErrorBanner error={deletePost.error} />
        </div>
      ) : null}
    </Card>
  )
}

interface BlogPostFormValues {
  title: string
  excerpt?: string
  body: string
}

function BlogPostForm({
  post,
  submitLabel,
  pending,
  error,
  onSubmit,
  onCancel,
}: {
  post?: AdminBlogPostDto
  submitLabel: string
  pending: boolean
  error: unknown
  onSubmit: (values: CreateBlogPostRequest & UpdateBlogPostRequest) => void
  onCancel: () => void
}) {
  const [title, setTitle] = useState(post?.title ?? '')
  const [excerpt, setExcerpt] = useState(post?.excerpt ?? '')
  const [body, setBody] = useState(post?.body ?? '')

  const submit = () => {
    const values: BlogPostFormValues = { title: title.trim(), excerpt: excerpt.trim() || undefined, body }
    onSubmit(values)
  }

  return (
    <div className="space-y-4">
      {error ? <ErrorBanner error={error} /> : null}
      <label className="block text-sm">
        <span className={LABEL}>Title</span>
        <input value={title} onChange={(event) => setTitle(event.target.value)} className={FIELD} />
      </label>
      <label className="block text-sm">
        <span className={LABEL}>Excerpt (optional, shown in the list)</span>
        <input value={excerpt} onChange={(event) => setExcerpt(event.target.value)} className={FIELD} />
      </label>
      <label className="block text-sm">
        <span className={LABEL}>Body</span>
        <textarea
          value={body}
          onChange={(event) => setBody(event.target.value)}
          rows={10}
          className={FIELD}
        />
        <span className="mt-1 block text-xs text-ink-muted">
          Separate paragraphs with a blank line.
        </span>
      </label>
      <div className="flex gap-3">
        <Button disabled={pending || !title.trim() || !body.trim()} onClick={submit}>
          {pending ? 'Saving…' : submitLabel}
        </Button>
        <Button variant="ghost" onClick={onCancel}>
          Cancel
        </Button>
      </div>
    </div>
  )
}
