import './StatusBadge.css'

// Maps every status value used across tenders/documents to a visual tone.
const TONE_MAP = {
  DRAFT: 'neutral',
  OPEN: 'verified',
  UNDER_REVIEW: 'pending',
  COMPLETED: 'neutral',
  PENDING: 'pending',
  VERIFIED: 'verified',
  FAILED: 'risk',
  REQUIRES_REVIEW: 'risk',
  MISMATCH: 'risk',
  NOT_FOUND: 'risk',
  NON_COMPLIANT: 'risk',
}

function formatLabel(status) {
  return status
    .toLowerCase()
    .split('_')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')
}

export default function StatusBadge({ status }) {
  if (!status) return null
  const tone = TONE_MAP[status] || 'neutral'
  return <span className={`status-badge status-badge--${tone}`}>{formatLabel(status)}</span>
}
