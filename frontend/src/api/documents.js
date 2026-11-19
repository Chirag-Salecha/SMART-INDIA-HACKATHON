import axios from 'axios'
import { apiClient } from './client.js'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

export const getDocumentsForBidder = (bidderId) =>
  axios.get(`${API_BASE_URL}/bidders/${bidderId}/documents`).then((res) => res.data)

// Uses a plain axios call (not the shared apiClient) so the browser can set
// the multipart/form-data boundary itself - the apiClient instance forces a
// JSON Content-Type header which would break file uploads.
export const uploadDocument = (bidderId, documentType, file) => {
  const formData = new FormData()
  formData.append('documentType', documentType)
  formData.append('file', file)

  return axios
    .post(`${API_BASE_URL}/bidders/${bidderId}/documents`, formData)
    .then((res) => res.data)
}

export const getDocument = (documentId) =>
  apiClient.get(`/documents/${documentId}`).then((res) => res.data)

export const reanalyzeDocument = (documentId) =>
  apiClient.post(`/documents/${documentId}/reanalyze`).then((res) => res.data)

export const getVerificationQueue = () =>
  apiClient.get('/documents/queue').then((res) => res.data)
