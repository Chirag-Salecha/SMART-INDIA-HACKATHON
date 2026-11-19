import { useState } from 'react'
import './OfficerReviewPanel.css'

const DECISION_LABELS = {
  APPROVE: 'Approve',
  REQUEST_CLARIFICATION: 'Request Clarification',
  REJECT: 'Reject',
}

const DECISION_TONE = {
  APPROVE: 'compliant',
  REQUEST_CLARIFICATION: 'review',
  REJECT: 'missing',
}

export default function OfficerReviewPanel({ history, onSubmit, submitting }) {
  const [officerName, setOfficerName] = useState('')
  const [comments, setComments] = useState('')

  const handleDecision = (decision) => {
    onSubmit({ decision, comments, officerName: officerName || 'Procurement Officer' })
    setComments('')
  }

  return (
    <div className="officer-review-panel">
      <div className="officer-review-disclaimer">
        Final procurement decision remains with the authorized Procurement Officer. The AI/rule engine
        above is a decision-support tool only and does not approve, reject, or disqualify any bidder.
      </div>

      <div className="form-grid" style={{ marginTop: 16 }}>
        <div className="form-row">
          <label className="form-field">
            <span>Officer Name</span>
            <input
              type="text"
              value={officerName}
              onChange={(e) => setOfficerName(e.target.value)}
              placeholder="e.g. R. Sharma"
            />
          </label>
        </div>

        <label className="form-field">
          <span>Comments / Reason</span>
          <textarea
            value={comments}
            onChange={(e) => setComments(e.target.value)}
            placeholder="Optional notes to accompany this decision"
          />
        </label>

        <div className="officer-review-actions">
          <button type="button" className="btn btn-primary" disabled={submitting} onClick={() => handleDecision('APPROVE')}>
            Approve
          </button>
          <button type="button" className="btn btn-secondary" disabled={submitting} onClick={() => handleDecision('REQUEST_CLARIFICATION')}>
            Request Clarification
          </button>
          <button type="button" className="btn btn-secondary officer-review-reject" disabled={submitting} onClick={() => handleDecision('REJECT')}>
            Reject
          </button>
        </div>
      </div>

      {history?.length > 0 && (
        <div className="officer-review-history">
          <h4>Decision History</h4>
          <ul>
            {history.map((entry) => (
              <li key={entry.id} className="officer-review-history-item">
                <span className={`officer-review-badge officer-review-badge--${DECISION_TONE[entry.decision] || 'review'}`}>
                  {DECISION_LABELS[entry.decision] || entry.decision}
                </span>
                <span className="officer-review-history-meta">
                  {entry.officerName || 'Unknown officer'} · {new Date(entry.decidedAt).toLocaleString()}
                  {entry.complianceScoreAtReview != null && ` · Score at review: ${entry.complianceScoreAtReview}/100 (${entry.riskLevelAtReview})`}
                </span>
                {entry.comments && <p className="officer-review-history-comment">{entry.comments}</p>}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}
