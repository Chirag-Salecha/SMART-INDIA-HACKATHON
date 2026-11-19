import Sidebar from './Sidebar.jsx'
import Topbar from './Topbar.jsx'
import './AppLayout.css'

/**
 * Shared shell for every authenticated page: sidebar + topbar + content
 * area. Introduced so Dashboard, Tenders, and their sub-pages all reuse the
 * same structure instead of duplicating it.
 */
export default function AppLayout({ eyebrow, title, actions, children }) {
  return (
    <div className="app-layout">
      <Sidebar />
      <div className="app-layout-main">
        <Topbar eyebrow={eyebrow} title={title} actions={actions} />
        <div className="app-layout-content">{children}</div>
      </div>
    </div>
  )
}
