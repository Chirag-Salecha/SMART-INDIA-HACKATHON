import { useState } from 'react'
import './RuleEngineSummary.css'

const RISK_TONE = { LOW: 'compliant', MEDIUM: 'review', HIGH: 'missing' }
const CATEGORY_ICON = {
  VERIFIED: '✓',
  CLEAR: '✓',
  PASS: '✓',
  PASSED: '✓',
  COMPLIANT: '✓',
  REVIEW: '⚠',
  REQUIRES_REVIEW: '⚠',
  UNVERIFIABLE: '⚠',
  MISMATCH: '✕',
  NOT_FOUND: '✕',
  NON_COMPLIANT: '✕',
  MISSING: '✕',
  FAILED: '✕',
  PENDING: '…',
  NOT_APPLICABLE: '–',
}
const CATEGORY_TONE = {
  VERIFIED: 'compliant',
  CLEAR: 'compliant',
  PASS: 'compliant',
  PASSED: 'compliant',
  COMPLIANT: 'compliant',
  REVIEW: 'review',
  REQUIRES_REVIEW: 'review',
  PENDING: 'review',
  UNVERIFIABLE: 'review',
  NOT_APPLICABLE: 'compliant',
  MISMATCH: 'missing',
  NOT_FOUND: 'missing',
  NON_COMPLIANT: 'missing',
  MISSING: 'missing',
  FAILED: 'missing',
}

function toneFor(status) {
  return CATEGORY_TONE[status] || 'missing'
}

const CATEGORY_LABELS = {
  GST: 'GST',
  PAN: 'PAN',
  UDYAM: 'Udyam',
  LOCAL_CONTENT: 'Local Content',
  DEBARMENT: 'Debarment',
  DOCUMENT_COMPLETENESS: 'Document Completeness',
  CROSS_DOCUMENT_MATCH: 'Cross-Document Match',
}

export default function RuleEngineSummary({ assessment }) {
  const [expanded, setExpanded] = useState(null)

  if (!assessment) return null

  return (
    <div className="rule-engine-summary">
      <div className="rule-engine-header">
        <div>
          <p className="rule-engine-label">Compliance Score</p>
          <p className="rule-engine-score">{assessment.complianceScore}/100</p>
          <p className="rule-engine-hint">Decision-support indicator only - not a sole qualification criterion.</p>
        </div>
        <div>
          <p className="rule-engine-label">Risk Level</p>
          <p className={`rule-engine-risk rule-engine-risk--${RISK_TONE[assessment.riskLevel] || 'review'}`}>
            {assessment.riskLevel}
          </p>
        </div>
      </div>

      {assessment.riskReasons?.length > 0 && (
        <div className="rule-engine-reasons">
          <h4>Risk Reasons</h4>
          <ul>
            {assessment.riskReasons.map((reason, idx) => (
              <li key={idx}>{reason}</li>
            ))}
          </ul>
        </div>
      )}

      {assessment.missingMandatoryDocuments?.length > 0 && (
        <div className="rule-engine-missing-docs">
          <strong>Missing Mandatory Documents:</strong>{' '}
          {assessment.missingMandatoryDocuments.map((d) => d.replace(/_/g, ' ')).join(', ')}
        </div>
      )}

      <ul className="rule-engine-categories">
        {assessment.categories.map((cat) => {
          const isOpen = expanded === cat.category
          return (
            <li key={cat.category} className="rule-engine-category">
              <button
                type="button"
                className="rule-engine-category-row"
                onClick={() => setExpanded(isOpen ? null : cat.category)}
              >
                <span className={`rule-engine-category-icon rule-engine-category-icon--${toneFor(cat.status)}`}>
                  {CATEGORY_ICON[cat.status] || '?'}
                </span>
                <span className="rule-engine-category-name">{CATEGORY_LABELS[cat.category] || cat.category}</span>
                <span className="rule-engine-category-weight">
                  {cat.achievedWeight.toFixed(1)} / {cat.weight}
                </span>
              </button>
              {isOpen && (
                <div className="rule-engine-category-detail">
                  <p><strong>Status:</strong> {cat.status.replace(/_/g, ' ')}</p>
                  <p><strong>Evidence:</strong> {cat.evidence}</p>
                  {cat.source && (
                    <p>
                      <strong>Source:</strong> {cat.source}
                      {cat.sourceType === 'REFERENCE_DATABASE' && ' (Prototype Reference Database — Synthetic)'}
                    </p>
                  )}
                </div>
              )}
            </li>
          )
        })}
      </ul>

      {assessment.recommendations?.length > 0 && (
        <div className="rule-engine-recommendations">
          <h4>AI Recommendations (Advisory Only)</h4>
          <ul>
            {assessment.recommendations.map((rec, idx) => (
              <li key={idx}>{rec}</li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}
