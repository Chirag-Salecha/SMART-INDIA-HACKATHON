import { apiClient } from './client.js'

export const getBidder = (id) => apiClient.get(`/bidders/${id}`).then((res) => res.data)

export const createBidder = (payload) => apiClient.post('/bidders', payload).then((res) => res.data)

export const updateBidder = (id, payload) =>
  apiClient.put(`/bidders/${id}`, payload).then((res) => res.data)

export const deleteBidder = (id) => apiClient.delete(`/bidders/${id}`)
