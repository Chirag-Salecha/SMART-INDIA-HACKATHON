import { apiClient } from './client.js'

export const submitOfficerReview = (bidderId, payload) =>
  apiClient.post(`/bidders/${bidderId}/review`, payload).then((res) => res.data)

export const getOfficerReviewHistory = (bidderId) =>
  apiClient.get(`/bidders/${bidderId}/review`).then((res) => res.data)
