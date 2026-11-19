import './Panel.css'

/**
 * Generic bordered content block used for lists/tables/forms across pages
 * (mirrors the visual language first used by ActivityLog on the Dashboard).
 */
export default function Panel({ title, subtitle, actions, children }) {
  return (
    <div className="panel">
      {(title || actions) && (
        <div className="panel-header">
          <div>
            {title && <h2>{title}</h2>}
            {subtitle && <p>{subtitle}</p>}
          </div>
          {actions && <div className="panel-actions">{actions}</div>}
        </div>
      )}
      <div className="panel-body">{children}</div>
    </div>
  )
}
