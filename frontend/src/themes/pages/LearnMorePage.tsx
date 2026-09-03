import { Link, Navigate, useParams } from 'react-router-dom'
import { themeBySlug } from '../themeConfig'
import { Card } from '../../components/ui/Card'

export function LearnMorePage() {
  const { slug } = useParams<{ slug: string }>()
  const themeEntry = themeBySlug(slug)

  if (!themeEntry) return <Navigate to="/" replace />

  return (
    <div className="mx-auto max-w-xl text-center">
      <Card className="p-10">
        <h1 className="mb-2 text-xl font-bold text-ink">{themeEntry.label}</h1>
        <p className="mb-6 text-sm text-ink-muted">
          Educational content for {themeEntry.label.toLowerCase()} is coming soon.
        </p>
        <Link to={`/themes/${slug}`} className="text-sm font-medium text-magenta-600 hover:underline">
          &larr; Back to {themeEntry.label}
        </Link>
      </Card>
    </div>
  )
}
