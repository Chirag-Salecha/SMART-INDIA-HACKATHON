import { useAuth } from '../context/AuthContext.jsx'
import './Topbar.css'

export default function Topbar({ eyebrow = 'Overview', title = 'Dashboard', actions }) {
  const { user, logout } = useAuth()

  return (
    <header className="topbar">
      <div>
        <p className="topbar-eyebrow">{eyebrow}</p>
        <h1 className="topbar-title">{title}</h1>
      </div>

      <div className="topbar-right">
        {actions && <div className="topbar-actions">{actions}</div>}

        <div className="topbar-user">
          <div className="topbar-user-info">
            <p className="topbar-user-name">{user?.username}</p>
            <p className="topbar-user-role">{user?.role}</p>
          </div>
          <button className="topbar-logout" onClick={logout}>
            Sign out
          </button>
        </div>
      </div>
    </header>
  )
}
