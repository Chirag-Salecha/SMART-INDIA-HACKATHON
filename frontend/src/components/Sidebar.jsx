import { NavLink } from 'react-router-dom'
import './Sidebar.css'

const NAV_ITEMS = [
  { label: 'Dashboard', path: '/dashboard' },
  { label: 'Tenders', path: '/tenders' },
  { label: 'Verification Queue', path: '/verification-queue' },
  { label: 'Compliance Reports', path: '/compliance-reports' },
  { label: 'Settings', path: null },
]

export default function Sidebar() {
  return (
    <aside className="sidebar">
      <div className="sidebar-brand">
        <svg width="24" height="24" viewBox="0 0 34 34" fill="none" aria-hidden="true">
          <rect x="1" y="1" width="32" height="32" stroke="#b78a2e" strokeWidth="1.5" />
          <path d="M9 22V12l8-5 8 5v10" stroke="#f6f4ee" strokeWidth="1.5" fill="none" />
          <path d="M9 22h16" stroke="#f6f4ee" strokeWidth="1.5" />
          <path d="M13 22v-6M17 22v-6M21 22v-6" stroke="#f6f4ee" strokeWidth="1.5" />
        </svg>
        <div>
          <p className="sidebar-brand-title">BCVP</p>
          <p className="sidebar-brand-subtitle">Bid Compliance</p>
        </div>
      </div>

      <nav className="sidebar-nav">
        {NAV_ITEMS.map((item) =>
          item.path ? (
            <NavLink
              key={item.label}
              to={item.path}
              className={({ isActive }) =>
                `sidebar-link${isActive ? ' sidebar-link--active' : ''}`
              }
            >
              {item.label}
            </NavLink>
          ) : (
            <a
              key={item.label}
              href="#"
              className="sidebar-link sidebar-link--disabled"
              onClick={(e) => e.preventDefault()}
            >
              {item.label}
            </a>
          )
        )}
      </nav>

      <div className="sidebar-footer">
        <p>Smart Automation</p>
        <p>SIH 2026</p>
      </div>
    </aside>
  )
}
