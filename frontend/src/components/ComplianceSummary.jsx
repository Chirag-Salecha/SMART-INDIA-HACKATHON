import { useState } from 'react'
import './ComplianceSummary.css'

const REQUIREMENT_ICON = {
  COMPLIANT: '✓',
  VERIFIED: '✓',
  CLEAR: '✓',
  PASS: '✓',
  PASSED: '✓',
  MISSING: '✕',
  NON_COMPLIANT: '✕',
  FAILED: '✕',
  MISMATCH: '✕',
  NOT_FOUND: '✕',
  REQUIRES_REVIEW: '⚠',
  REVIEW: '⚠',
  UNVERIFIABLE: '⚠',
  PENDING: '…',
  NOT_APPLICABLE: '–',
}

const REQUIREMENT_TONE = {
  COMPLIANT: 'compliant',
  VERIFIED: 'compliant',
  CLEAR: 'compliant',
  PASS: 'compliant',
  PASSED: 'compliant',
  MISSING: 'missing',
  NON_COMPLIANT: 'missing',
  FAILED: 'missing',
  MISMATCH: 'missing',
  NOT_FOUND: 'missing',
  REQUIRES_REVIEW: 'review',
  REVIEW: 'review',
  UNVERIFIABLE: 'review',
  PENDING: 'review',
  NOT_APPLICABLE: 'compliant',
}

function riskTone(risk) {
  if (risk === 'LOW') return 'compliant'
  if (risk === 'MEDIUM') return 'review'
  return 'missing'
}

export default function ComplianceSummary({ report }) {
  const [expanded, setExpanded] = useState(null)

  if (!report) return null

  const items = report.requirements || report.categories || []

  return (
    <div className="compliance-summary">
      <div className="compliance-score-row">
        <div>
          <p className="compliance-score-label">Compliance Score</p>
          <p className="compliance-score-value">{report.complianceScore}/100</p>
        </div>
        <div>
          <p className="compliance-score-label">Risk Level</p>
          <p className={`compliance-risk-value compliance-risk--${riskTone(report.riskLevel)}`}>
            {report.riskLevel}
          </p>
        </div>
      </div>

      <ul className="compliance-requirements">
        {items.map((req) => {
          const name = req.requirement || req.category || ''
          const tone = REQUIREMENT_TONE[req.status] || 'review'
          const isOpen = expanded === name
          const achieved = req.achievedWeight ?? req.score
          const max = req.weight ?? req.maxScore
          const weightLabel = achieved !== undefined && max !== undefined
            ? `${Number(achieved).toFixed(0)}/${max} pts`
            : `${max ?? req.weight} pts`

          return (
            <li key={name} className="compliance-requirement">
              <button
                type="button"
                className="compliance-requirement-row"
                onClick={() => setExpanded(isOpen ? null : name)}
              >
                <span className={`compliance-requirement-icon compliance-requirement-icon--${tone}`}>
                  {REQUIREMENT_ICON[req.status] || '⚠'}
                </span>
                <span className="compliance-requirement-name">{name.replace(/_/g, ' ')}</span>
                <span className="compliance-requirement-weight">{weightLabel}</span>
              </button>

              {isOpen && (
                <div className="compliance-requirement-detail">
                  <p><strong>Status:</strong> {(req.status || '').replace(/_/g, ' ')}</p>
                  <p><strong>Evidence:</strong> {req.evidence}</p>
                  <p>
                    <strong>Risk:</strong>{' '}
                    {req.risk ||
                      (['VERIFIED', 'COMPLIANT', 'CLEAR'].includes(req.status)
                        ? 'LOW'
                        : ['REQUIRES_REVIEW', 'REVIEW', 'UNVERIFIABLE', 'PENDING'].includes(req.status)
                        ? 'MEDIUM'
                        : 'HIGH')}
                  </p>
                  {req.recommendation && (
                    <p><strong>Recommendation:</strong> {req.recommendation}</p>
                  )}
                  {req.source && (
                    <p><strong>Source:</strong> {req.source}</p>
                  )}
                </div>
              )}
            </li>
          )
        })}
      </ul>
    </div>
  )
}
