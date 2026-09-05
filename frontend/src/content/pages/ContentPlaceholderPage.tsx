import { Card } from '../../components/ui/Card'

/**
 * About, Contact, and Blog exist because the nav promises them. The copy is a placeholder - real
 * content replaces it the same way survey content does, by being written rather than invented here.
 */
export function ContentPlaceholderPage({ title, blurb }: { title: string; blurb: string }) {
  return (
    <div className="mx-auto max-w-2xl">
      <h1 className="mb-2 text-2xl font-bold text-ink">{title}</h1>
      <p className="mb-8 text-sm text-ink-muted">{blurb}</p>
      <Card className="p-10 text-center">
        <p className="text-sm text-ink-muted">This page is waiting on its content.</p>
      </Card>
    </div>
  )
}

export function AboutPage() {
  return (
    <ContentPlaceholderPage
      title="About"
      blurb="Who we are and how Bellavera approaches personalized women's health."
    />
  )
}

export function ContactPage() {
  return (
    <ContentPlaceholderPage title="Contact" blurb="How to reach us." />
  )
}

export function BlogPage() {
  return (
    <ContentPlaceholderPage title="Blog" blurb="Notes, research, and updates from the team." />
  )
}
