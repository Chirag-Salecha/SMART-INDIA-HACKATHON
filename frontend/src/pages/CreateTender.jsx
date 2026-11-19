import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import AppLayout from '../components/AppLayout.jsx'
import Panel from '../components/Panel.jsx'
import { createTender, getTender, updateTender } from '../api/tenders.js'
import { TENDER_STATUSES } from '../constants/options.js'
import '../styles/forms.css'

const INITIAL_FORM = {
  tenderNumber: '',
  title: '',
  description: '',
  department: '',
  deadline: '',
  status: 'DRAFT',
  localContentThresholdPercent: '',
}

export default function CreateTender() {
  const { id } = useParams()
  const isEdit = Boolean(id)
  const navigate = useNavigate()

  const [form, setForm] = useState(INITIAL_FORM)
  const [loading, setLoading] = useState(isEdit)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (isEdit) {
      setLoading(true)
      getTender(id)
        .then((data) => {
          setForm({
            tenderNumber: data.tenderNumber || '',
            title: data.title || '',
            description: data.description || '',
            department: data.department || '',
            deadline: data.deadline ? data.deadline.slice(0, 16) : '',
            status: data.status || 'DRAFT',
            localContentThresholdPercent:
              data.localContentThresholdPercent != null ? String(data.localContentThresholdPercent) : '',
          })
        })
        .catch(() => setError('Could not load tender details to edit.'))
        .finally(() => setLoading(false))
    }
  }, [id, isEdit])

  const handleChange = (field) => (e) => {
    setForm((prev) => ({ ...prev, [field]: e.target.value }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSubmitting(true)
    setError('')

    try {
      const payload = {
        ...form,
        // datetime-local gives "YYYY-MM-DDTHH:mm" which the backend accepts as LocalDateTime
        deadline: form.deadline || null,
        localContentThresholdPercent: form.localContentThresholdPercent
          ? parseInt(form.localContentThresholdPercent, 10)
          : null,
      }
      if (isEdit) {
        await updateTender(id, payload)
        navigate(`/tenders/${id}`)
      } else {
        const tender = await createTender(payload)
        navigate(`/tenders/${tender.id}`)
      }
    } catch (err) {
      const message =
        err.response?.data?.fieldErrors
          ? Object.values(err.response.data.fieldErrors).join(', ')
          : err.response?.data?.message || (isEdit ? 'Could not update tender.' : 'Could not create tender.')
      setError(message)
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return (
      <AppLayout eyebrow="Procurement" title="Edit Tender">
        <Panel><div className="data-table-empty">Loading tender details…</div></Panel>
      </AppLayout>
    )
  }

  return (
    <AppLayout eyebrow="Procurement" title={isEdit ? `Edit Tender: ${form.tenderNumber || ''}` : 'Create Tender'}>
      <Panel
        title={isEdit ? 'Update Tender Details' : 'Tender Details'}
        subtitle="Fields marked required must be filled in"
      >
        <form className="form-grid" style={{ padding: '24px' }} onSubmit={handleSubmit}>
          {error && <div className="form-error">{error}</div>}

          <div className="form-row">
            <label className="form-field">
              <span>Tender Number *</span>
              <input
                type="text"
                value={form.tenderNumber}
                onChange={handleChange('tenderNumber')}
                placeholder="TDR-2026-0150"
                required
              />
            </label>

            <label className="form-field">
              <span>Department</span>
              <input
                type="text"
                value={form.department}
                onChange={handleChange('department')}
                placeholder="Public Works Department"
              />
            </label>
          </div>

          <label className="form-field">
            <span>Title *</span>
            <input
              type="text"
              value={form.title}
              onChange={handleChange('title')}
              placeholder="Supply and installation of solar street lights"
              required
            />
          </label>

          <label className="form-field">
            <span>Description</span>
            <textarea
              value={form.description}
              onChange={handleChange('description')}
              placeholder="Scope of work, eligibility criteria, and other details"
            />
          </label>

          <div className="form-row">
            <label className="form-field">
              <span>Submission Deadline *</span>
              <input
                type="datetime-local"
                value={form.deadline}
                onChange={handleChange('deadline')}
                required
              />
            </label>

            <label className="form-field">
              <span>Status</span>
              <select value={form.status} onChange={handleChange('status')}>
                {TENDER_STATUSES.map((status) => (
                  <option key={status} value={status}>
                    {status.replace('_', ' ')}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <div className="form-row">
            <label className="form-field">
              <span>Min. Local Content Threshold (%)</span>
              <input
                type="number"
                min="0"
                max="100"
                value={form.localContentThresholdPercent}
                onChange={handleChange('localContentThresholdPercent')}
                placeholder="e.g. 50"
              />
            </label>
          </div>

          <div className="form-actions">
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? 'Saving…' : isEdit ? 'Save Changes' : 'Create Tender'}
            </button>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => navigate(isEdit ? `/tenders/${id}` : '/tenders')}
            >
              Cancel
            </button>
          </div>
        </form>
      </Panel>
    </AppLayout>
  )
}
