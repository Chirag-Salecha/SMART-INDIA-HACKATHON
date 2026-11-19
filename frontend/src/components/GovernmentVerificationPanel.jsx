import { useState } from 'react'
import StatusBadge from './StatusBadge.jsx'
import DemoVerificationNotice from './DemoVerificationNotice.jsx'
import './GovernmentVerificationPanel.css'

const TYPE_LABELS = {
  GST: 'GST',
  PAN: 'PAN',
  UDYAM: 'Udyam',
  BLACKLISTING: 'Debarment / Blacklisting',
  MAKE_IN_INDIA: 'Local Content (Make in India)',
  CROSS_SOURCE: 'Cross-Source Consistency',
}

const STATUS_ICON = {
  VERIFIED: '✓',
  MISMATCH: '✕',
  NOT_FOUND: '✕',
  NON_COMPLIANT: '✕',
  REQUIRES_REVIEW: '⚠',
  UNVERIFIABLE: '–',
}

function GovernmentVerificationCard({ item }) {
  return (
    <div className="gov-verify-card">
      <div className="gov-verify-card-header">
        <span className="gov-verify-card-type">{TYPE_LABELS[item.verificationType] || item.verificationType}</span>
        {item.identifierChecked && <span className="gov-verify-card-identifier">{item.identifierChecked}</span>}
      </div>

      <div className="gov-verify-card-status">
        <span className={`gov-verify-icon gov-verify-icon--${item.resultStatus === 'VERIFIED' ? 'ok' : item.resultStatus === 'REQUIRES_REVIEW' || item.resultStatus === 'UNVERIFIABLE' ? 'warn' : 'bad'}`}>
          {STATUS_ICON[item.resultStatus] || '?'}
        </span>
        <StatusBadge status={item.resultStatus} />
      </div>

      {item.governmentValue && (
        <p className="gov-verify-line">
          <strong>Government record:</strong> {item.governmentValue}
        </p>
      )}
      {item.matchedValue && item.verificationType !== 'CROSS_SOURCE' && (
        <p className="gov-verify-line">
          <strong>Bidder profile:</strong> {item.matchedValue}
        </p>
      )}
      {item.discrepancyDescription && (
        <p className="gov-verify-line gov-verify-line--issue">{item.discrepancyDescription}</p>
      )}

      <div className="gov-verify-card-footer">
        {item.source && <span>Source: {item.source}</span>}
        {item.verifiedAt && <span>Last verified: {new Date(item.verifiedAt).toLocaleString()}</span>}
      </div>
    </div>
  )
}

export default function GovernmentVerificationPanel({ summary, onVerify, verifying }) {
  const items = summary?.items || []
  const coreItems = items.filter((i) => i.verificationType !== 'CROSS_SOURCE')
  const crossSourceItem = items.find((i) => i.verificationType === 'CROSS_SOURCE')

  return (
    <div className="gov-verify-panel">
      <div style={{ marginBottom: 16 }}>
        <DemoVerificationNotice />
      </div>

      <div className="gov-verify-toolbar">
        {summary?.overallStatus && (
          <div className="gov-verify-overall">
            <span>Overall:</span>
            <StatusBadge status={summary.overallStatus} />
          </div>
        )}
        <button type="button" className="btn btn-primary" onClick={onVerify} disabled={verifying}>
          {verifying ? 'Verifying…' : 'Verify Government Records'}
        </button>
      </div>

      {coreItems.length === 0 ? (
        <div className="data-table-empty">
          No government verification has been run yet for this bidder.
        </div>
      ) : (
        <div className="gov-verify-grid">
          {coreItems.map((item) => (
            <GovernmentVerificationCard key={item.verificationType} item={item} />
          ))}
        </div>
      )}

      {crossSourceItem && (
        <div className="gov-verify-cross-source">
          <GovernmentVerificationCard item={crossSourceItem} />
        </div>
      )}
    </div>
  )
}
