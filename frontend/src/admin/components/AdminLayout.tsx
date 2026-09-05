import { NavLink, Outlet } from 'react-router-dom'

const TABS = [
  { label: 'Surveys', to: '/admin/surveys' },
  { label: 'Products', to: '/admin/products' },
  { label: 'Fulfillment', to: '/admin/orders' },
  { label: 'Users', to: '/admin/users' },
] as const

export function AdminLayout() {
  return (
    <div>
      <h1 className="mb-1 text-2xl font-bold text-ink">Admin console</h1>
      <p className="mb-6 text-sm text-ink-muted">
        Survey authoring, the product catalog, the shipping queue, and accounts.
      </p>

      <div className="mb-8 flex gap-1 border-b border-surface-border">
        {TABS.map((tab) => (
          <NavLink
            key={tab.to}
            to={tab.to}
            className={({ isActive }) =>
              [
                '-mb-px border-b-2 px-4 py-2.5 text-sm font-semibold transition-colors',
                isActive
                  ? 'border-magenta-500 text-magenta-600'
                  : 'border-transparent text-ink-muted hover:text-ink',
              ].join(' ')
            }
          >
            {tab.label}
          </NavLink>
        ))}
      </div>

      <Outlet />
    </div>
  )
}
