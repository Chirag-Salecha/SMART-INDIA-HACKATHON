import { apiClient } from './client.js'

export const getTenders = () => apiClient.get('/tenders').then((res) => res.data)

export const getTender = (id) => apiClient.get(`/tenders/${id}`).then((res) => res.data)

export const createTender = (payload) => apiClient.post('/tenders', payload).then((res) => res.data)

export const updateTender = (id, payload) =>
  apiClient.put(`/tenders/${id}`, payload).then((res) => res.data)

export const deleteTender = (id) => apiClient.delete(`/tenders/${id}`)

export const getBiddersForTender = (tenderId) =>
  apiClient.get(`/tenders/${tenderId}/bidders`).then((res) => res.data)
