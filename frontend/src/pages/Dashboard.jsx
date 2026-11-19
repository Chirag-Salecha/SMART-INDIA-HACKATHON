import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import AppLayout from '../components/AppLayout.jsx'
import StatCard from '../components/StatCard.jsx'
import ActivityLog from '../components/ActivityLog.jsx'
import Panel from '../components/Panel.jsx'
import DataTable from '../components/DataTable.jsx'
import { getDashboardStats, getDashboardComplianceStats } from '../api/dashboard.js'
import './Dashboard.css'
import '../styles/forms.css'

// Mock data for the MVP milestone. Will be replaced by a real activity feed
// (e.g. recent document verifications) in a later increment.
const ACTIVITY = [
  { id: 1, bidder: 'Meridian Infra Pvt. Ltd.', tenderId: 'TDR-2026-0142', status: 'Verified', reviewedAt: '10 min ago' },
  { id: 2, bidder: 'Sundaram Electricals', tenderId: 'TDR-2026-0139', status: 'Pending', reviewedAt: '42 min ago' },
  { id: 3, bidder: 'Kavya Construction Co.', tenderId: 'TDR-2026-0138', status: 'High Risk', reviewedAt: '1 hr ago' },
  { id: 4, bidder: 'Nilgiri Roadworks Ltd.', tenderId: 'TDR-2026-0135', status: 'Verified', reviewedAt: '2 hr ago' },
  { id: 5, bidder: 'Anantha Steel Traders', tenderId: 'TDR-2026-0131', status: 'Pending', reviewedAt: '3 hr ago' },
  { id: 6, bidder: 'Vardhan Logistics', tenderId: 'TDR-2026-0128', status: 'Verified', reviewedAt: '5 hr ago' },
]

const RISK_COLOR = { LOW: 'compliant', MEDIUM: 'review', HIGH: 'missing' }

const BIDDER_COLUMNS = [
  { key: 'companyName', header: 'Company Name' },
  { key: 'tenderNumber', header: 'Tender', render: (row) => row.tenderNumber || '—' },
  { key: 'complianceScore', header: 'Compliance Score', render: (row) => `${row.complianceScore}/100` },
  {
    key: 'riskLevel',
    header: 'Risk Level',
    render: (row) => (
      <span className={`dashboard-risk-pill dashboard-risk-pill--${RISK_COLOR[row.riskLevel] || 'review'}`}>
        {row.riskLevel}
      </span>
    ),
  },
  {
    key: 'missingMandatoryDocuments',
    header: 'Missing Documents',
    render: (row) =>
      row.missingMandatoryDocuments?.length
        ? row.missingMandatoryDocuments.map((d) => d.replace(/_/g, ' ')).join(', ')
        : 'None',
  },
]

export default function Dashboard() {
  const [stats, setStats] = useState(null)
  const [complianceStats, setComplianceStats] = useState(null)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  useEffect(() => {
    getDashboardStats()
      .then(setStats)
      .catch(() => setError('Could not load live stats. Is the backend running?'))

    getDashboardComplianceStats()
      .then(setComplianceStats)
      .catch(() => setComplianceStats(null))
  }, [])

  const statCards = [
    { label: 'Total Tenders', value: stats?.totalTenders ?? '—', tone: 'neutral' },
    { label: 'Total Bidders', value: stats?.totalBidders ?? '—', tone: 'neutral' },
    { label: 'Pending Documents', value: stats?.pendingDocuments ?? '—', tone: 'pending' },
    { label: 'Verified Documents', value: stats?.verifiedDocuments ?? '—', tone: 'verified' },
  ]

  const complianceCards = complianceStats && [
    { label: 'Total Bidders', value: complianceStats.totalBidders, tone: 'neutral' },
    { label: 'Compliant', value: complianceStats.compliant, tone: 'verified' },
    { label: 'Needs Review', value: complianceStats.needsReview, tone: 'pending' },
    { label: 'High Risk', value: complianceStats.highRisk, tone: 'risk' },
    { label: 'Missing Documents', value: complianceStats.missingDocuments, tone: 'risk' },
  ]

  return (
    <AppLayout eyebrow="Overview" title="Dashboard">
      {error && <div className="form-error">{error}</div>}

      <section className="dashboard-stats">
        {statCards.map((stat) => (
          <StatCard key={stat.label} {...stat} />
        ))}
      </section>

      <Panel
        title="AI Bid Compliance Dashboard"
        subtitle="Compliance and risk across every bidder, from the rule engine (GST, PAN, Udyam, Local Content, Debarment, cross-document consistency)"
      >
        {complianceCards ? (
          <>
            <div className="dashboard-compliance-stats">
              {complianceCards.map((stat) => (
                <StatCard key={stat.label} {...stat} />
              ))}
            </div>
            <DataTable
              columns={BIDDER_COLUMNS}
              rows={complianceStats.bidders.map((b) => ({ ...b, id: b.bidderId }))}
              onRowClick={(row) => navigate(`/bidders/${row.bidderId}`)}
              emptyMessage="No bidders yet."
            />
          </>
        ) : (
          <div className="data-table-empty">Compliance dashboard will appear once bidders exist.</div>
        )}
      </Panel>

      <ActivityLog entries={ACTIVITY} />
    </AppLayout>
  )
}
