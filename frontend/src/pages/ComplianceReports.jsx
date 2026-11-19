import { useEffect, useState } from 'react'
import AppLayout from '../components/AppLayout.jsx'
import Panel from '../components/Panel.jsx'
import DataTable from '../components/DataTable.jsx'
import DemoVerificationNotice from '../components/DemoVerificationNotice.jsx'
import ComplianceSummary from '../components/ComplianceSummary.jsx'
import { getTenders } from '../api/tenders.js'
import { getComplianceForTender } from '../api/compliance.js'
import '../styles/forms.css'
import './ComplianceReports.css'

const RISK_COLOR = { LOW: 'compliant', MEDIUM: 'review', HIGH: 'missing' }

const COLUMNS = [
  { key: 'companyName', header: 'Bidder' },
  { key: 'complianceScore', header: 'Score', render: (row) => `${row.complianceScore}/100` },
  {
    key: 'riskLevel',
    header: 'Risk',
    render: (row) => (
      <span className={`compliance-risk-pill compliance-risk-pill--${RISK_COLOR[row.riskLevel] || 'review'}`}>
        {row.riskLevel}
      </span>
    ),
  },
]

export default function ComplianceReports() {
  const [tenders, setTenders] = useState([])
  const [selectedTenderId, setSelectedTenderId] = useState('')
  const [reports, setReports] = useState([])
  const [selectedBidderId, setSelectedBidderId] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    getTenders().then((data) => {
      setTenders(data)
      if (data.length > 0) setSelectedTenderId(String(data[0].id))
    })
  }, [])

  useEffect(() => {
    if (!selectedTenderId) return
    setLoading(true)
    setSelectedBidderId(null)
    getComplianceForTender(selectedTenderId)
      .then((data) => {
        setReports(data.map((r) => ({ ...r, id: r.bidderId })))
        setError('')
      })
      .catch(() => setError('Could not load compliance data for this tender.'))
      .finally(() => setLoading(false))
  }, [selectedTenderId])

  const selectedReport = reports.find((r) => r.bidderId === selectedBidderId)

  return (
    <AppLayout eyebrow="Procurement" title="Compliance Reports">
      <DemoVerificationNotice />

      <Panel
        title="Select a Tender"
        actions={
          <button className="btn btn-secondary" onClick={() => window.print()}>
            Print Report
          </button>
        }
      >
        <div style={{ padding: '20px 24px' }}>
          <select
            className="compliance-tender-select"
            value={selectedTenderId}
            onChange={(e) => setSelectedTenderId(e.target.value)}
          >
            {tenders.map((t) => (
              <option key={t.id} value={t.id}>
                {t.tenderNumber} — {t.title}
              </option>
            ))}
          </select>
        </div>
      </Panel>

      <Panel title="Bidder Compliance" subtitle="Click a bidder to see full requirement-by-requirement evidence">
        {error && <div className="form-error" style={{ margin: '20px 24px' }}>{error}</div>}
        {!error && (
          <DataTable
            columns={COLUMNS}
            rows={reports}
            onRowClick={(row) => setSelectedBidderId(row.bidderId === selectedBidderId ? null : row.bidderId)}
            emptyMessage={loading ? 'Loading…' : 'No bidders for this tender yet.'}
          />
        )}
      </Panel>

      {selectedReport && (
        <Panel title={`${selectedReport.companyName} — Detailed Report`}>
          <ComplianceSummary report={selectedReport} />
        </Panel>
      )}
    </AppLayout>
  )
}
