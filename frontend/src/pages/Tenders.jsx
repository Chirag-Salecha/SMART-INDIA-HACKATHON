import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import AppLayout from '../components/AppLayout.jsx'
import Panel from '../components/Panel.jsx'
import DataTable from '../components/DataTable.jsx'
import StatusBadge from '../components/StatusBadge.jsx'
import { deleteTender, getTenders } from '../api/tenders.js'

export default function Tenders() {
  const [tenders, setTenders] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  const loadTenders = () => {
    setLoading(true)
    getTenders()
      .then(setTenders)
      .catch(() => setError('Could not load tenders. Is the backend running?'))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    loadTenders()
  }, [])

  const handleDelete = async (tenderId, tenderNumber) => {
    const confirmed = window.confirm(
      `Are you sure you want to delete tender "${tenderNumber}"? All associated bidders, documents, and reviews will be permanently removed.`
    )
    if (!confirmed) return

    try {
      await deleteTender(tenderId)
      setTenders((prev) => prev.filter((t) => t.id !== tenderId))
    } catch (err) {
      alert(err.response?.data?.message || 'Could not delete tender. Please try again.')
    }
  }

  const columns = useMemo(
    () => [
      { key: 'tenderNumber', header: 'Tender Number' },
      { key: 'title', header: 'Title' },
      { key: 'department', header: 'Department' },
      {
        key: 'deadline',
        header: 'Deadline',
        render: (row) => (row.deadline ? new Date(row.deadline).toLocaleString() : '—'),
      },
      {
        key: 'status',
        header: 'Status',
        render: (row) => <StatusBadge status={row.status} />,
      },
      {
        key: 'actions',
        header: 'Actions',
        render: (row) => (
          <div style={{ display: 'flex', gap: '8px' }} onClick={(e) => e.stopPropagation()}>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={(e) => {
                e.stopPropagation()
                navigate(`/tenders/${row.id}/edit`)
              }}
            >
              Edit
            </button>
            <button
              type="button"
              className="btn btn-danger btn-sm"
              onClick={(e) => {
                e.stopPropagation()
                handleDelete(row.id, row.tenderNumber)
              }}
            >
              Delete
            </button>
          </div>
        ),
      },
    ],
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [navigate]
  )

  return (
    <AppLayout
      eyebrow="Procurement"
      title="Tenders"
      actions={
        <button className="btn btn-primary" onClick={() => navigate('/tenders/new')}>
          Create Tender
        </button>
      }
    >
      <Panel title="All Tenders" subtitle="Every tender currently tracked by the platform">
        {error && <div className="form-error" style={{ margin: '20px 24px' }}>{error}</div>}
        {!error && (
          <DataTable
            columns={columns}
            rows={tenders}
            onRowClick={(row) => navigate(`/tenders/${row.id}`)}
            emptyMessage={loading ? 'Loading tenders…' : 'No tenders yet. Create the first one to get started.'}
          />
        )}
      </Panel>
    </AppLayout>
  )
}
