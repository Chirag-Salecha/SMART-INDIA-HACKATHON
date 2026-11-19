import { useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import AppLayout from '../components/AppLayout.jsx'
import Panel from '../components/Panel.jsx'
import DemoVerificationNotice from '../components/DemoVerificationNotice.jsx'
import ComplianceSummary from '../components/ComplianceSummary.jsx'
import DocumentVerificationList from '../components/DocumentVerificationList.jsx'
import GovernmentVerificationPanel from '../components/GovernmentVerificationPanel.jsx'
import RuleEngineSummary from '../components/RuleEngineSummary.jsx'
import OfficerReviewPanel from '../components/OfficerReviewPanel.jsx'
import { getBidder, updateBidder, deleteBidder } from '../api/bidders.js'
import { getDocumentsForBidder, uploadDocument, reanalyzeDocument } from '../api/documents.js'
import { getComplianceForBidder } from '../api/compliance.js'
import { getGovernmentVerification, runGovernmentVerification } from '../api/governmentVerification.js'
import { getComplianceAssessment } from '../api/ruleEngine.js'
import { submitOfficerReview, getOfficerReviewHistory } from '../api/officerReview.js'
import { DOCUMENT_TYPES } from '../constants/options.js'
import './BidderDetails.css'
import '../styles/forms.css'

export default function BidderDetails() {
  const { id } = useParams()
  const navigate = useNavigate()
  const fileInputRef = useRef(null)

  const [bidder, setBidder] = useState(null)
  const [documents, setDocuments] = useState([])
  const [compliance, setCompliance] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [isEditing, setIsEditing] = useState(false)
  const [editForm, setEditForm] = useState({
    companyName: '',
    panNumber: '',
    gstin: '',
    udyamNumber: '',
    email: '',
    phone: '',
  })
  const [editSubmitting, setEditSubmitting] = useState(false)
  const [editError, setEditError] = useState('')

  const [documentType, setDocumentType] = useState(DOCUMENT_TYPES[0])
  const [selectedFile, setSelectedFile] = useState(null)
  const [uploading, setUploading] = useState(false)
  const [uploadError, setUploadError] = useState('')
  const [analyzingId, setAnalyzingId] = useState(null)

  const [govVerification, setGovVerification] = useState(null)
  const [verifyingGov, setVerifyingGov] = useState(false)

  const [assessment, setAssessment] = useState(null)
  const [reviewHistory, setReviewHistory] = useState([])
  const [submittingReview, setSubmittingReview] = useState(false)

  const loadData = () => {
    setLoading(true)
    Promise.all([getBidder(id), getDocumentsForBidder(id)])
      .then(([bidderData, documentsData]) => {
        setBidder(bidderData)
        setDocuments(documentsData)
        setError('')
      })
      .catch(() => setError('Could not load this bidder. They may not exist.'))
      .finally(() => setLoading(false))

    // Compliance depends on the tender's requirements + documents, so it's
    // fetched independently and allowed to fail quietly (e.g. before any
    // documents exist) without blocking the rest of the page.
    getComplianceForBidder(id)
      .then(setCompliance)
      .catch(() => setCompliance(null))

    getGovernmentVerification(id)
      .then(setGovVerification)
      .catch(() => setGovVerification(null))

    getComplianceAssessment(id)
      .then(setAssessment)
      .catch(() => setAssessment(null))

    getOfficerReviewHistory(id)
      .then(setReviewHistory)
      .catch(() => setReviewHistory([]))
  }

  useEffect(() => {
    loadData()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id])

  const handleUpload = async (e) => {
    e.preventDefault()
    if (!selectedFile) {
      setUploadError('Please choose a file to upload.')
      return
    }

    setUploading(true)
    setUploadError('')

    try {
      await uploadDocument(id, documentType, selectedFile)
      setSelectedFile(null)
      if (fileInputRef.current) fileInputRef.current.value = ''
      loadData()
    } catch (err) {
      setUploadError(err.response?.data?.message || 'Upload failed. Please try again.')
    } finally {
      setUploading(false)
    }
  }

  const handleAnalyze = async (documentId) => {
    setAnalyzingId(documentId)
    try {
      await reanalyzeDocument(documentId)
      loadData()
    } catch (err) {
      setError(err.response?.data?.message || 'Analysis failed. Please try again.')
    } finally {
      setAnalyzingId(null)
    }
  }

  const handleVerifyGovernment = async () => {
    setVerifyingGov(true)
    try {
      const summary = await runGovernmentVerification(id)
      setGovVerification(summary)
      // Government checks feed the rule engine's score, so refresh it too.
      getComplianceAssessment(id).then(setAssessment).catch(() => {})
      getComplianceForBidder(id).then(setCompliance).catch(() => {})
    } catch (err) {
      setError(err.response?.data?.message || 'Government verification failed. Please try again.')
    } finally {
      setVerifyingGov(false)
    }
  }

  const handleOfficerReview = async (payload) => {
    setSubmittingReview(true)
    try {
      await submitOfficerReview(id, payload)
      const history = await getOfficerReviewHistory(id)
      setReviewHistory(history)
    } catch (err) {
      setError(err.response?.data?.message || 'Could not record the review decision. Please try again.')
    } finally {
      setSubmittingReview(false)
    }
  }

  const handleStartEdit = () => {
    setEditForm({
      companyName: bidder.companyName || '',
      panNumber: bidder.panNumber || '',
      gstin: bidder.gstin || '',
      udyamNumber: bidder.udyamNumber || '',
      email: bidder.email || '',
      phone: bidder.phone || '',
    })
    setEditError('')
    setIsEditing(true)
  }

  const handleCancelEdit = () => {
    setIsEditing(false)
    setEditError('')
  }

  const handleEditChange = (field) => (e) => {
    setEditForm((prev) => ({ ...prev, [field]: e.target.value }))
  }

  const handleSaveEdit = async (e) => {
    e.preventDefault()
    setEditSubmitting(true)
    setEditError('')

    try {
      const updated = await updateBidder(id, {
        ...editForm,
        tenderId: bidder.tenderId,
      })
      setBidder(updated)
      setIsEditing(false)
    } catch (err) {
      const message =
        err.response?.data?.fieldErrors
          ? Object.values(err.response.data.fieldErrors).join(', ')
          : err.response?.data?.message || 'Could not update bidder. Please try again.'
      setEditError(message)
    } finally {
      setEditSubmitting(false)
    }
  }

  const handleDeleteBidder = async () => {
    const confirmed = window.confirm(
      `Are you sure you want to delete bidder "${bidder.companyName}"? All documents, verification history, and officer reviews will be permanently removed.`
    )
    if (!confirmed) return

    try {
      await deleteBidder(id)
      navigate(`/tenders/${bidder.tenderId}`)
    } catch (err) {
      setError(err.response?.data?.message || 'Could not delete bidder. Please try again.')
    }
  }

  if (loading) {
    return (
      <AppLayout eyebrow="Procurement" title="Bidder Details">
        <Panel><div className="data-table-empty">Loading bidder…</div></Panel>
      </AppLayout>
    )
  }

  if (error || !bidder) {
    return (
      <AppLayout eyebrow="Procurement" title="Bidder Details">
        <Panel><div className="form-error" style={{ margin: 24 }}>{error}</div></Panel>
      </AppLayout>
    )
  }

  return (
    <AppLayout
      eyebrow="Procurement"
      title={bidder.companyName}
      actions={
        <div style={{ display: 'flex', gap: '10px' }}>
          <button
            className="btn btn-secondary"
            onClick={isEditing ? handleCancelEdit : handleStartEdit}
          >
            {isEditing ? 'Cancel Edit' : 'Edit Bidder'}
          </button>
          <button className="btn btn-danger" onClick={handleDeleteBidder}>
            Delete Bidder
          </button>
          <button className="btn btn-secondary" onClick={() => navigate(`/tenders/${bidder.tenderId}`)}>
            Back to Tender
          </button>
        </div>
      }
    >
      <Panel
        title="Bidder Information"
        actions={
          <button
            className="btn btn-secondary btn-sm"
            onClick={isEditing ? handleCancelEdit : handleStartEdit}
          >
            {isEditing ? 'Cancel' : 'Edit Info'}
          </button>
        }
      >
        {isEditing ? (
          <form className="form-grid" style={{ padding: '24px' }} onSubmit={handleSaveEdit}>
            {editError && <div className="form-error">{editError}</div>}

            <div className="form-row">
              <label className="form-field">
                <span>Company Name *</span>
                <input
                  type="text"
                  value={editForm.companyName}
                  onChange={handleEditChange('companyName')}
                  required
                />
              </label>
              <label className="form-field">
                <span>Email</span>
                <input
                  type="email"
                  value={editForm.email}
                  onChange={handleEditChange('email')}
                />
              </label>
            </div>

            <div className="form-row">
              <label className="form-field">
                <span>PAN Number</span>
                <input
                  type="text"
                  value={editForm.panNumber}
                  onChange={handleEditChange('panNumber')}
                />
              </label>
              <label className="form-field">
                <span>GSTIN</span>
                <input
                  type="text"
                  value={editForm.gstin}
                  onChange={handleEditChange('gstin')}
                />
              </label>
            </div>

            <div className="form-row">
              <label className="form-field">
                <span>Udyam Number</span>
                <input
                  type="text"
                  value={editForm.udyamNumber}
                  onChange={handleEditChange('udyamNumber')}
                />
              </label>
              <label className="form-field">
                <span>Phone</span>
                <input
                  type="text"
                  value={editForm.phone}
                  onChange={handleEditChange('phone')}
                />
              </label>
            </div>

            <div className="form-actions">
              <button type="submit" className="btn btn-primary" disabled={editSubmitting}>
                {editSubmitting ? 'Saving…' : 'Save Changes'}
              </button>
              <button type="button" className="btn btn-secondary" onClick={handleCancelEdit}>
                Cancel
              </button>
            </div>
          </form>
        ) : (
          <dl className="bidder-info-grid">
            <div>
              <dt>PAN Number</dt>
              <dd>{bidder.panNumber || '—'}</dd>
            </div>
            <div>
              <dt>GSTIN</dt>
              <dd>{bidder.gstin || '—'}</dd>
            </div>
            <div>
              <dt>Udyam Number</dt>
              <dd>{bidder.udyamNumber || '—'}</dd>
            </div>
            <div>
              <dt>Email</dt>
              <dd>{bidder.email || '—'}</dd>
            </div>
            <div>
              <dt>Phone</dt>
              <dd>{bidder.phone || '—'}</dd>
            </div>
            <div>
              <dt>Registered</dt>
              <dd>{bidder.createdAt ? new Date(bidder.createdAt).toLocaleString() : '—'}</dd>
            </div>
          </dl>
        )}
      </Panel>

      <Panel title="Government Verification" subtitle="Simulated cross-check against GST, PAN and Udyam records">
        <GovernmentVerificationPanel
          summary={govVerification}
          onVerify={handleVerifyGovernment}
          verifying={verifyingGov}
        />
      </Panel>

      <Panel title="Compliance Summary" subtitle="Derived from the tender's requirements and this bidder's documents">
        <div style={{ padding: '0 24px 20px' }}>
          <DemoVerificationNotice />
        </div>
        {(compliance || assessment) ? (
          <ComplianceSummary report={compliance || assessment} />
        ) : (
          <div className="data-table-empty">Compliance score will appear once documents are uploaded.</div>
        )}
      </Panel>

      <Panel title="AI Bid Compliance Assessment" subtitle="Combines GST, PAN, Udyam, Local Content, Debarment, cross-document consistency and missing documents into one score">
        {(assessment || compliance) ? (
          <RuleEngineSummary assessment={assessment || compliance} />
        ) : (
          <div className="data-table-empty">Assessment will appear once available for this bidder.</div>
        )}
      </Panel>

      <Panel title="Procurement Officer Review" subtitle="The final decision always rests with the Procurement Officer">
        <OfficerReviewPanel
          history={reviewHistory}
          onSubmit={handleOfficerReview}
          submitting={submittingReview}
        />
      </Panel>

      <Panel title="Upload Document" subtitle="PDF, PNG or JPEG - up to 10MB">
        <form className="form-grid" style={{ padding: '24px' }} onSubmit={handleUpload}>
          {uploadError && <div className="form-error">{uploadError}</div>}

          <div className="form-row">
            <label className="form-field">
              <span>Document Type</span>
              <select value={documentType} onChange={(e) => setDocumentType(e.target.value)}>
                {DOCUMENT_TYPES.map((type) => (
                  <option key={type} value={type}>
                    {type.replace(/_/g, ' ')}
                  </option>
                ))}
              </select>
            </label>

            <label className="form-field">
              <span>File</span>
              <input
                ref={fileInputRef}
                type="file"
                accept=".pdf,.png,.jpg,.jpeg"
                onChange={(e) => setSelectedFile(e.target.files?.[0] || null)}
              />
            </label>
          </div>

          <div className="form-actions">
            <button type="submit" className="btn btn-primary" disabled={uploading}>
              {uploading ? 'Uploading & Analyzing…' : 'Upload Document'}
            </button>
          </div>
          <p className="form-hint">
            Documents are automatically sent for AI verification right after upload.
          </p>
        </form>
      </Panel>

      <Panel title="Documents" subtitle={`${documents.length} document(s) uploaded`}>
        <DocumentVerificationList
          documents={documents}
          onAnalyze={handleAnalyze}
          analyzingId={analyzingId}
        />
      </Panel>
    </AppLayout>
  )
}
