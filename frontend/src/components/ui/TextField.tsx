import type { InputHTMLAttributes } from 'react'

interface TextFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string
}

export function TextField({ label, id, className = '', ...props }: TextFieldProps) {
  const inputId = id ?? props.name
  return (
    <label htmlFor={inputId} className="block text-sm">
      <span className="mb-1.5 block font-medium text-ink">{label}</span>
      <input
        id={inputId}
        className={`w-full rounded-lg border border-surface-border bg-white px-3 py-2.5 text-ink outline-none focus:border-magenta-500 focus:ring-1 focus:ring-magenta-500 ${className}`}
        {...props}
      />
    </label>
  )
}
