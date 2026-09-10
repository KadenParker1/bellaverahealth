import { Card } from '../../components/ui/Card'

export function AboutPage() {
  return (
    <div className="mx-auto max-w-2xl">
      <h1 className="mb-2 text-2xl font-bold text-ink">About Bellavera</h1>
      <p className="mb-8 text-sm text-ink-muted">
        Personalized women's health guidance, grounded in what you tell us.
      </p>

      <Card className="space-y-4 p-8 text-sm text-ink">
        <p>
          Bellavera brings together a short onboarding survey, four themed check-ins - Exercise,
          Nutrition, Hormones, and Pelvic Floor - and a chat assistant that uses your own answers as
          context. The goal is guidance that reflects your actual situation, not generic advice.
        </p>
        <p>
          The assistant is built to be supportive and informative, never diagnostic. It treats
          everything you share as self-reported, and it will point you toward in-person care for
          anything that needs a clinician's attention rather than a chatbot's.
        </p>
        <p>
          Alongside the guidance, our store carries a small set of physical products relevant to
          the areas we cover, with order tracking and support handled right in your account.
        </p>
      </Card>
    </div>
  )
}
