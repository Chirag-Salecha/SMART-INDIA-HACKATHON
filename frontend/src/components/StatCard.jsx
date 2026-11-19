import './StatCard.css'

/**
 * Reusable stat block for dashboard KPIs.
 * `tone` controls the accent color: 'neutral' | 'verified' | 'pending' | 'risk'
 */
export default function StatCard({ label, value, caption, tone = 'neutral' }) {
  return (
    <div className={`stat-card stat-card--${tone}`}>
      <p className="stat-card-label">{label}</p>
      <p className="stat-card-value">{value}</p>
      {caption && <p className="stat-card-caption">{caption}</p>}
    </div>
  )
}
