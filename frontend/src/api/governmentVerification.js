import { apiClient } from './client.js'

export const getGovernmentVerification = (bidderId) =>
  apiClient.get(`/bidders/${bidderId}/government-verification`).then((res) => res.data)

export const runGovernmentVerification = (bidderId) =>
  apiClient.post(`/bidders/${bidderId}/government-verification/verify`).then((res) => res.data)
