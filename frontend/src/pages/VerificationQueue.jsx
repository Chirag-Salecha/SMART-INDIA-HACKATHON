import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import AppLayout from '../components/AppLayout.jsx'
import Panel from '../components/Panel.jsx'
import DataTable from '../components/DataTable.jsx'
import StatusBadge from '../components/StatusBadge.jsx'
import DemoVerificationNotice from '../components/DemoVerificationNotice.jsx'
import { getVerificationQueue } from '../api/documents.js'
import '../styles/forms.css'

const COLUMNS = [
  { key: 'companyName', header: 'Bidder' },
  { key: 'tenderNumber', header: 'Tender' },
  {
    key: 'documentType',
    header: 'Document Type',
    render: (row) => row.documentType.replace(/_/g, ' '),
  },
  {
    key: 'uploadedAt',
    header: 'Uploaded',
    render: (row) => (row.uploadedAt ? new Date(row.uploadedAt).toLocaleString() : '—'),
  },
  {
    key: 'verificationStatus',
    header: 'Status',
    render: (row) => <StatusBadge status={row.verificationStatus} />,
  },
]

export default function VerificationQueue() {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  useEffect(() => {
    getVerificationQueue()
      .then((data) => setItems(data.map((item) => ({ ...item, id: item.documentId }))))
      .catch(() => setError('Could not load the verification queue. Is the backend running?'))
      .finally(() => setLoading(false))
  }, [])

  return (
    <AppLayout eyebrow="Procurement" title="Verification Queue">
      <DemoVerificationNotice />

      <Panel
        title="Documents Needing Attention"
        subtitle="Pending, requires-review, and failed documents across every tender"
      >
        {error && <div className="form-error" style={{ margin: '20px 24px' }}>{error}</div>}
        {!error && (
          <DataTable
            columns={COLUMNS}
            rows={items}
            onRowClick={(row) => navigate(`/bidders/${row.bidderId}`)}
            emptyMessage={loading ? 'Loading queue…' : 'Nothing needs attention right now.'}
          />
        )}
      </Panel>
    </AppLayout>
  )
}
