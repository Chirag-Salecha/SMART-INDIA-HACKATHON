import { apiClient } from './client.js'

export const getComplianceForBidder = (bidderId) =>
  apiClient.get(`/compliance/bidders/${bidderId}`).then((res) => res.data)

export const getComplianceForTender = (tenderId) =>
  apiClient.get(`/compliance/tenders/${tenderId}`).then((res) => res.data)
