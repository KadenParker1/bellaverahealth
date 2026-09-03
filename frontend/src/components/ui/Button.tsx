import type { ButtonHTMLAttributes } from 'react'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'ghost'
}

const base =
  'inline-flex items-center justify-center rounded-lg px-4 py-2.5 text-sm font-semibold transition-colors disabled:opacity-50 disabled:cursor-not-allowed'

const variants: Record<NonNullable<ButtonProps['variant']>, string> = {
  primary: 'bg-magenta-500 text-white hover:bg-magenta-600',
  secondary:
    'bg-white text-ink border border-surface-border hover:bg-surface-subtle',
  ghost: 'text-magenta-600 hover:bg-magenta-100',
}

export function Button({ variant = 'primary', className = '', ...props }: ButtonProps) {
  return <button className={`${base} ${variants[variant]} ${className}`} {...props} />
}
