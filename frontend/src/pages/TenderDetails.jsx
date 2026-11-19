import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import AppLayout from '../components/AppLayout.jsx'
import Panel from '../components/Panel.jsx'
import DataTable from '../components/DataTable.jsx'
import StatusBadge from '../components/StatusBadge.jsx'
import { getTender, getBiddersForTender, deleteTender } from '../api/tenders.js'
import { createBidder, updateBidder, deleteBidder } from '../api/bidders.js'
import './TenderDetails.css'
import '../styles/forms.css'

const INITIAL_BIDDER_FORM = {
  companyName: '',
  panNumber: '',
  gstin: '',
  udyamNumber: '',
  email: '',
  phone: '',
}

export default function TenderDetails() {
  const { id } = useParams()
  const navigate = useNavigate()

  const [tender, setTender] = useState(null)
  const [bidders, setBidders] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [showBidderForm, setShowBidderForm] = useState(false)
  const [editingBidderId, setEditingBidderId] = useState(null)
  const [bidderForm, setBidderForm] = useState(INITIAL_BIDDER_FORM)
  const [submitting, setSubmitting] = useState(false)
  const [formError, setFormError] = useState('')

  const loadData = () => {
    setLoading(true)
    Promise.all([getTender(id), getBiddersForTender(id)])
      .then(([tenderData, biddersData]) => {
        setTender(tenderData)
        setBidders(biddersData)
        setError('')
      })
      .catch(() => setError('Could not load this tender. It may not exist.'))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    loadData()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id])

  const handleBidderChange = (field) => (e) => {
    setBidderForm((prev) => ({ ...prev, [field]: e.target.value }))
  }

  const handleDeleteTender = async () => {
    const confirmed = window.confirm(
      `Are you sure you want to delete tender "${tender.tenderNumber}"? All associated bidders, documents, and reviews will be permanently removed.`
    )
    if (!confirmed) return

    try {
      await deleteTender(id)
      navigate('/tenders')
    } catch (err) {
      setError(err.response?.data?.message || 'Could not delete tender. Please try again.')
    }
  }

  const handleDeleteBidder = async (bidderId, companyName) => {
    const confirmed = window.confirm(
      `Are you sure you want to delete bidder "${companyName}"? All uploaded documents and verifications will be permanently removed.`
    )
    if (!confirmed) return

    try {
      await deleteBidder(bidderId)
      setBidders((prev) => prev.filter((b) => b.id !== bidderId))
    } catch (err) {
      alert(err.response?.data?.message || 'Could not delete bidder. Please try again.')
    }
  }

  const startEditBidder = (row) => {
    setEditingBidderId(row.id)
    setBidderForm({
      companyName: row.companyName || '',
      panNumber: row.panNumber || '',
      gstin: row.gstin || '',
      udyamNumber: row.udyamNumber || '',
      email: row.email || '',
      phone: row.phone || '',
    })
    setFormError('')
    setShowBidderForm(true)
  }

  const cancelBidderForm = () => {
    setShowBidderForm(false)
    setEditingBidderId(null)
    setBidderForm(INITIAL_BIDDER_FORM)
    setFormError('')
  }

  const handleSaveBidder = async (e) => {
    e.preventDefault()
    setSubmitting(true)
    setFormError('')

    try {
      const payload = { ...bidderForm, tenderId: Number(id) }
      if (editingBidderId) {
        await updateBidder(editingBidderId, payload)
      } else {
        await createBidder(payload)
      }
      cancelBidderForm()
      loadData()
    } catch (err) {
      const message =
        err.response?.data?.fieldErrors
          ? Object.values(err.response.data.fieldErrors).join(', ')
          : err.response?.data?.message || 'Could not save bidder. Please try again.'
      setFormError(message)
    } finally {
      setSubmitting(false)
    }
  }

  const bidderColumns = useMemo(
    () => [
      { key: 'companyName', header: 'Company Name' },
      { key: 'panNumber', header: 'PAN', render: (row) => row.panNumber || '—' },
      { key: 'gstin', header: 'GSTIN', render: (row) => row.gstin || '—' },
      { key: 'email', header: 'Email', render: (row) => row.email || '—' },
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
                startEditBidder(row)
              }}
            >
              Edit
            </button>
            <button
              type="button"
              className="btn btn-danger btn-sm"
              onClick={(e) => {
                e.stopPropagation()
                handleDeleteBidder(row.id, row.companyName)
              }}
            >
              Delete
            </button>
          </div>
        ),
      },
    ],
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [bidders]
  )

  if (loading) {
    return (
      <AppLayout eyebrow="Procurement" title="Tender Details">
        <Panel><div className="data-table-empty">Loading tender…</div></Panel>
      </AppLayout>
    )
  }

  if (error || !tender) {
    return (
      <AppLayout eyebrow="Procurement" title="Tender Details">
        <Panel><div className="form-error" style={{ margin: 24 }}>{error}</div></Panel>
      </AppLayout>
    )
  }

  return (
    <AppLayout
      eyebrow="Procurement"
      title={tender.tenderNumber}
      actions={
        <div style={{ display: 'flex', gap: '10px' }}>
          <button className="btn btn-secondary" onClick={() => navigate(`/tenders/${id}/edit`)}>
            Edit Tender
          </button>
          <button className="btn btn-danger" onClick={handleDeleteTender}>
            Delete Tender
          </button>
          <button className="btn btn-secondary" onClick={() => navigate('/tenders')}>
            Back to Tenders
          </button>
        </div>
      }
    >
      <Panel title={tender.title} subtitle={tender.department || 'No department specified'}>
        <dl className="tender-info-grid">
          <div>
            <dt>Status</dt>
            <dd><StatusBadge status={tender.status} /></dd>
          </div>
          <div>
            <dt>Deadline</dt>
            <dd>{tender.deadline ? new Date(tender.deadline).toLocaleString() : '—'}</dd>
          </div>
          <div>
            <dt>Created</dt>
            <dd>{tender.createdAt ? new Date(tender.createdAt).toLocaleString() : '—'}</dd>
          </div>
          <div className="tender-info-description">
            <dt>Description</dt>
            <dd>{tender.description || 'No description provided.'}</dd>
          </div>
        </dl>
      </Panel>

      <Panel
        title="Bidders"
        subtitle={`${bidders.length} bidder(s) submitted for this tender`}
        actions={
          <button
            className="btn btn-primary"
            onClick={() => {
              if (showBidderForm) {
                cancelBidderForm()
              } else {
                setEditingBidderId(null)
                setBidderForm(INITIAL_BIDDER_FORM)
                setFormError('')
                setShowBidderForm(true)
              }
            }}
          >
            {showBidderForm ? 'Cancel' : 'Add Bidder'}
          </button>
        }
      >
        {showBidderForm && (
          <form className="form-grid" style={{ padding: '24px' }} onSubmit={handleSaveBidder}>
            {formError && <div className="form-error">{formError}</div>}

            <div className="form-row">
              <label className="form-field">
                <span>Company Name *</span>
                <input
                  type="text"
                  value={bidderForm.companyName}
                  onChange={handleBidderChange('companyName')}
                  required
                />
              </label>
              <label className="form-field">
                <span>Email</span>
                <input
                  type="email"
                  value={bidderForm.email}
                  onChange={handleBidderChange('email')}
                />
              </label>
            </div>

            <div className="form-row">
              <label className="form-field">
                <span>PAN Number</span>
                <input
                  type="text"
                  value={bidderForm.panNumber}
                  onChange={handleBidderChange('panNumber')}
                />
              </label>
              <label className="form-field">
                <span>GSTIN</span>
                <input
                  type="text"
                  value={bidderForm.gstin}
                  onChange={handleBidderChange('gstin')}
                />
              </label>
            </div>

            <div className="form-row">
              <label className="form-field">
                <span>Udyam Number</span>
                <input
                  type="text"
                  value={bidderForm.udyamNumber}
                  onChange={handleBidderChange('udyamNumber')}
                />
              </label>
              <label className="form-field">
                <span>Phone</span>
                <input
                  type="text"
                  value={bidderForm.phone}
                  onChange={handleBidderChange('phone')}
                />
              </label>
            </div>

            <div className="form-actions">
              <button type="submit" className="btn btn-primary" disabled={submitting}>
                {submitting ? 'Saving…' : editingBidderId ? 'Update Bidder' : 'Save Bidder'}
              </button>
              {editingBidderId && (
                <button type="button" className="btn btn-secondary" onClick={cancelBidderForm}>
                  Cancel
                </button>
              )}
            </div>
          </form>
        )}

        <DataTable
          columns={bidderColumns}
          rows={bidders}
          onRowClick={(row) => navigate(`/bidders/${row.id}`)}
          emptyMessage="No bidders yet for this tender."
        />
      </Panel>
    </AppLayout>
  )
}
