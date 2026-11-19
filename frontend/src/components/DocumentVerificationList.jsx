import { useState } from 'react'
import StatusBadge from './StatusBadge.jsx'
import './DocumentVerificationList.css'

const CHECK_STATUS_TONE = {
  MATCH: 'compliant',
  INFO: 'compliant',
  MISMATCH: 'missing',
  MISSING_INFORMATION: 'review',
}

export default function DocumentVerificationList({ documents, onAnalyze, analyzingId }) {
  const [expandedId, setExpandedId] = useState(null)

  if (!documents || documents.length === 0) {
    return <div className="data-table-empty">No documents uploaded yet.</div>
  }

  return (
    <ul className="doc-verify-list">
      {documents.map((doc) => {
        const isOpen = expandedId === doc.id
        const verification = doc.verification
        const isAnalyzing = analyzingId === doc.id

        return (
          <li key={doc.id} className="doc-verify-item">
            <button
              type="button"
              className="doc-verify-row"
              onClick={() => setExpandedId(isOpen ? null : doc.id)}
            >
              <div className="doc-verify-main">
                <span className="doc-verify-name">{doc.originalFileName}</span>
                <span className="doc-verify-type">{doc.documentType.replace(/_/g, ' ')}</span>
              </div>
              <div className="doc-verify-meta">
                {verification?.confidence != null && (
                  <span className="doc-verify-confidence">
                    {Math.round(verification.confidence * 100)}% confidence
                  </span>
                )}
                <StatusBadge status={doc.verificationStatus} />
              </div>
            </button>

            {isOpen && (
              <div className="doc-verify-detail">
                <div className="doc-verify-detail-actions">
                  <button
                    type="button"
                    className="btn btn-secondary"
                    disabled={isAnalyzing}
                    onClick={(e) => {
                      e.stopPropagation()
                      onAnalyze(doc.id)
                    }}
                  >
                    {isAnalyzing ? 'Analyzing…' : 'Verify / Re-analyze'}
                  </button>
                  <span className="doc-verify-source">
                    Source: {verification?.verificationSource || 'Not yet analyzed'}
                  </span>
                </div>

                {verification?.errorMessage && (
                  <div className="form-error">{verification.errorMessage}</div>
                )}

                {verification?.extractedData && (
                  <div className="doc-verify-section">
                    <h4>Extracted Information</h4>
                    <dl className="doc-verify-fields">
                      {Object.entries(verification.extractedData)
                        .filter(([, value]) => value !== null && value !== undefined && value !== '')
                        .map(([key, value]) => (
                          <div key={key}>
                            <dt>{key.replace(/([A-Z])/g, ' $1').replace(/^./, (c) => c.toUpperCase())}</dt>
                            <dd>{String(value)}</dd>
                          </div>
                        ))}
                    </dl>
                  </div>
                )}

                {verification?.checks?.length > 0 && (
                  <div className="doc-verify-section">
                    <h4>Checks</h4>
                    <ul className="doc-verify-checks">
                      {verification.checks.map((check, idx) => (
                        <li key={idx} className={`doc-verify-check doc-verify-check--${CHECK_STATUS_TONE[check.status] || 'review'}`}>
                          <span className="doc-verify-check-field">{check.field}</span>
                          <span className="doc-verify-check-status">{check.status.replace(/_/g, ' ')}</span>
                          <p className="doc-verify-check-evidence">{check.evidence}</p>
                        </li>
                      ))}
                    </ul>
                  </div>
                )}

                {verification?.issues?.length > 0 && (
                  <div className="doc-verify-section">
                    <h4>Issues</h4>
                    <ul className="doc-verify-plain-list doc-verify-plain-list--issues">
                      {verification.issues.map((issue, idx) => (
                        <li key={idx}>{issue}</li>
                      ))}
                    </ul>
                  </div>
                )}

                {verification?.recommendations?.length > 0 && (
                  <div className="doc-verify-section">
                    <h4>Recommendations</h4>
                    <ul className="doc-verify-plain-list">
                      {verification.recommendations.map((rec, idx) => (
                        <li key={idx}>{rec}</li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            )}
          </li>
        )
      })}
    </ul>
  )
}
